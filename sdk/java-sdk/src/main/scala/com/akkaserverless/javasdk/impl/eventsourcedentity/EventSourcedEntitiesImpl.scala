/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.akkaserverless.javasdk.impl.eventsourcedentity

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Source
import com.akkaserverless.javasdk.AkkaServerlessRunner.Configuration
import com.akkaserverless.javasdk.eventsourcedentity._
import com.akkaserverless.javasdk.impl._
import com.akkaserverless.javasdk.impl.effect.EffectSupport
import com.akkaserverless.javasdk.impl.effect.ErrorReplyImpl
import com.akkaserverless.javasdk.impl.effect.MessageReplyImpl
import com.akkaserverless.javasdk.impl.effect.SecondaryEffectImpl
import com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityHandler.CommandResult
import com.akkaserverless.javasdk.ComponentOptions
import com.akkaserverless.javasdk.Context
import com.akkaserverless.javasdk.Metadata
import com.akkaserverless.javasdk.Service
import com.akkaserverless.javasdk.ServiceCallFactory
import com.akkaserverless.protocol.event_sourced_entity.EventSourcedStreamIn.Message.{ Init => InInit }
import com.akkaserverless.protocol.event_sourced_entity.EventSourcedStreamIn.Message.{ Empty => InEmpty }
import com.akkaserverless.protocol.event_sourced_entity.EventSourcedStreamIn.Message.{ Command => InCommand }
import com.akkaserverless.protocol.event_sourced_entity.EventSourcedStreamIn.Message.{ Event => InEvent }
import com.akkaserverless.protocol.event_sourced_entity.EventSourcedStreamOut.Message.{ Reply => OutReply }
import com.akkaserverless.protocol.event_sourced_entity.EventSourcedStreamOut.Message.{ Failure => OutFailure }
import com.akkaserverless.protocol.event_sourced_entity._
import com.google.protobuf.any.{ Any => ScalaPbAny }
import com.google.protobuf.Descriptors
import com.google.protobuf.{ Any => JavaPbAny }
import scala.util.control.NonFatal

import com.akkaserverless.javasdk.impl.EventSourcedEntityFactory

final class EventSourcedEntityService(
    val factory: EventSourcedEntityFactory,
    override val descriptor: Descriptors.ServiceDescriptor,
    val anySupport: AnySupport,
    override val entityType: String,
    val snapshotEvery: Int, // FIXME remove and only use entityOptions snapshotEvery?
    val entityOptions: Option[EventSourcedEntityOptions])
    extends Service {

  def this(
      factory: EventSourcedEntityFactory,
      descriptor: Descriptors.ServiceDescriptor,
      anySupport: AnySupport,
      entityType: String,
      snapshotEvery: Int,
      entityOptions: EventSourcedEntityOptions) =
    this(factory, descriptor, anySupport, entityType, snapshotEvery, Some(entityOptions))

  override def resolvedMethods: Option[Map[String, ResolvedServiceMethod[_, _]]] =
    factory match {
      case resolved: ResolvedEntityFactory => Some(resolved.resolvedMethods)
      case _                               => None
    }

  override final val componentType = EventSourcedEntities.name

  def withSnapshotEvery(snapshotEvery: Int): EventSourcedEntityService =
    if (snapshotEvery != this.snapshotEvery)
      new EventSourcedEntityService(
        this.factory,
        this.descriptor,
        this.anySupport,
        this.entityType,
        snapshotEvery,
        this.entityOptions)
    else
      this

  override def componentOptions: Option[ComponentOptions] = entityOptions
}

final class EventSourcedEntitiesImpl(
    system: ActorSystem,
    _services: Map[String, EventSourcedEntityService],
    rootContext: Context,
    configuration: Configuration)
    extends EventSourcedEntities {
  import EntityExceptions._

  private val log = Logging(system.eventStream, this.getClass)
  private final val services = _services.iterator.map { case (name, service) =>
    if (service.snapshotEvery < 0)
      log.warning("Snapshotting disabled for entity [{}], this is not recommended.", service.entityType)
    // FIXME overlay configuration provided by _system
    (name, if (service.snapshotEvery == 0) service.withSnapshotEvery(configuration.snapshotEvery) else service)
  }.toMap

  /**
   * The stream. One stream will be established per active entity. Once established, the first message sent will be
   * Init, which contains the entity ID, and, if the entity has previously persisted a snapshot, it will contain that
   * snapshot. It will then send zero to many event messages, one for each event previously persisted. The entity is
   * expected to apply these to its state in a deterministic fashion. Once all the events are sent, one to many commands
   * are sent, with new commands being sent as new requests for the entity come in. The entity is expected to reply to
   * each command with exactly one reply message. The entity should reply in order, and any events that the entity
   * requests to be persisted the entity should handle itself, applying them to its own state, as if they had arrived as
   * events when the event stream was being replayed on load.
   */
  override def handle(in: akka.stream.scaladsl.Source[EventSourcedStreamIn, akka.NotUsed])
      : akka.stream.scaladsl.Source[EventSourcedStreamOut, akka.NotUsed] =
    in.prefixAndTail(1)
      .flatMapConcat {
        case (Seq(EventSourcedStreamIn(InInit(init), _)), source) =>
          source.via(runEntity(init))
        case (Seq(), _) =>
          // if error during recovery in proxy the stream will be completed before init
          log.warning("Event Sourced Entity stream closed before init.")
          Source.empty[EventSourcedStreamOut]
        case (Seq(EventSourcedStreamIn(other, _)), _) =>
          throw ProtocolException(
            s"Expected init message for Event Sourced Entity, but received [${other.getClass.getName}]")
      }
      .recover { case error =>
        log.error(error, failureMessage(error))
        EventSourcedStreamOut(OutFailure(failure(error)))
      }

  private def runEntity(init: EventSourcedInit): Flow[EventSourcedStreamIn, EventSourcedStreamOut, NotUsed] = {
    val service =
      services.getOrElse(init.serviceName, throw ProtocolException(init, s"Service not found: ${init.serviceName}"))
    val handler = service.factory
      .create(new EventSourcedEntityContextImpl(init.entityId))
      .asInstanceOf[EventSourcedEntityHandler[Any, EventSourcedEntity[Any]]]
    val thisEntityId = init.entityId

    val startingSequenceNumber = (for {
      snapshot <- init.snapshot
      any <- snapshot.snapshot
    } yield {
      val snapshotSequence = snapshot.snapshotSequence
      handler._internalHandleSnapshot(service.anySupport.decode(ScalaPbAny.toJavaProto(any)))
      snapshotSequence
    }).getOrElse(0L)

    Flow[EventSourcedStreamIn]
      .map(_.message)
      .scan[(Long, Option[EventSourcedStreamOut.Message])]((startingSequenceNumber, None)) {
        case (_, InEvent(event)) =>
          // Note that these only come on replay
          val context = new EventContextImpl(thisEntityId, event.sequence)
          val ev =
            service.anySupport.decode(ScalaPbAny.toJavaProto(event.payload.get)).asInstanceOf[AnyRef] // FIXME empty?
          handler._internalHandleEvent(ev, context)
          (event.sequence, None)
        case ((sequence, _), InCommand(command)) =>
          if (thisEntityId != command.entityId)
            throw ProtocolException(command, "Receiving entity is not the intended recipient of command")

          val cmd =
            service.anySupport.decode(
              ScalaPbAny.toJavaProto(command.payload.getOrElse(throw ProtocolException(command, "No command payload"))))
          val metadata = new MetadataImpl(command.metadata.map(_.entries.toVector).getOrElse(Nil))
          val context =
            new CommandContextImpl(thisEntityId, sequence, command.name, command.id, metadata)

          // FIXME we'd want to somehow share this handle-command-apply-event logic to get the end effect ready for asserting in the testkit
          // FIXME a bit mixed concerns here, esp with the serialization to PbAny but it's either that or pushing this into the handler and making
          // SecondaryEffectImpl a public API (or make handler internal, which may be a good idea, also for the testkit)
          val CommandResult(
            events: Vector[Any],
            secondaryEffect: SecondaryEffectImpl,
            snapshot: Option[Any],
            endSequenceNumber) =
            try {
              handler._internalHandleCommand(
                command.name,
                cmd,
                context,
                service.snapshotEvery,
                seqNr => new EventContextImpl(thisEntityId, seqNr))
            } catch {
              case e: EntityException => throw e
              case NonFatal(error) =>
                throw EntityException(command, s"Unexpected failure: $error", Some(error))
            } finally {
              context.deactivate() // Very important!
            }

          val serializedSecondaryEffect = secondaryEffect match {
            case MessageReplyImpl(message, metadata, sideEffects) =>
              MessageReplyImpl(service.anySupport.encodeJava(message), metadata, sideEffects)
            case other => other
          }

          val clientAction =
            serializedSecondaryEffect.replyToClientAction(
              command.id,
              allowNoReply = false,
              restartOnFailure = events.nonEmpty)

          serializedSecondaryEffect match {
            case error: ErrorReplyImpl[_] =>
              log.error(
                "Fail invoked for command [{}] for entity [{}]: {}",
                command.name,
                thisEntityId,
                error.description)
              (
                endSequenceNumber,
                Some(OutReply(EventSourcedReply(commandId = command.id, clientAction = clientAction))))

            case _ => // non-error
              val serializedEvents = events.map(event => ScalaPbAny.fromJavaProto(service.anySupport.encodeJava(event)))
              val serializedSnapshot =
                snapshot.map(state => ScalaPbAny.fromJavaProto(service.anySupport.encodeJava(state)))
              (
                endSequenceNumber,
                Some(
                  OutReply(
                    EventSourcedReply(
                      command.id,
                      clientAction,
                      EffectSupport.sideEffectsFrom(serializedSecondaryEffect),
                      serializedEvents,
                      serializedSnapshot))))
          }
        case (_, InInit(_)) =>
          throw ProtocolException(init, "Entity already inited")
        case (_, InEmpty) =>
          throw ProtocolException(init, "Received empty/unknown message")
      }
      .collect { case (_, Some(message)) =>
        EventSourcedStreamOut(message)
      }
  }

  private class CommandContextImpl(
      override val entityId: String,
      override val sequenceNumber: Long,
      override val commandName: String,
      override val commandId: Long,
      override val metadata: Metadata)
      extends AbstractContext(rootContext.serviceCallFactory(), system)
      with CommandContext
      with ActivatableContext

  private class EventSourcedEntityContextImpl(override final val entityId: String)
      extends AbstractContext(rootContext.serviceCallFactory(), system)
      with EventSourcedEntityContext
  private final class EventContextImpl(entityId: String, override val sequenceNumber: Long)
      extends EventSourcedEntityContextImpl(entityId)
      with EventContext
}
