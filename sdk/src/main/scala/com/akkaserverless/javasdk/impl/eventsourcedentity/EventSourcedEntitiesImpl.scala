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
import akka.event.{Logging, LoggingAdapter}
import akka.stream.scaladsl.Flow
import com.akkaserverless.javasdk.AkkaServerlessRunner.Configuration
import com.akkaserverless.javasdk.eventsourcedentity._
import com.akkaserverless.javasdk.impl._
import com.akkaserverless.javasdk.impl.reply.ReplySupport
import com.akkaserverless.javasdk.{Context, Metadata, Reply, Service, ServiceCallFactory}
import com.akkaserverless.protocol.event_sourced_entity.EventSourcedStreamIn.Message.{
  Command => InCommand,
  Empty => InEmpty,
  Event => InEvent,
  Init => InInit
}
import com.akkaserverless.protocol.event_sourced_entity.EventSourcedStreamOut.Message.{
  Failure => OutFailure,
  Reply => OutReply
}
import com.akkaserverless.protocol.event_sourced_entity._
import com.google.protobuf.any.{Any => ScalaPbAny}
import com.google.protobuf.{Descriptors, Any => JavaPbAny}

import scala.util.control.NonFatal
import akka.stream.scaladsl.Source
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityBase.Effect
import com.akkaserverless.javasdk.impl.effect.EffectSupport
import com.akkaserverless.javasdk.impl.effect.ErrorReplyImpl
import com.akkaserverless.javasdk.impl.effect.MessageReplyImpl
import com.akkaserverless.javasdk.impl.effect.SecondaryEffectImpl
import com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl.EmitEvents
import com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl.NoPrimaryEffect
import com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl.PrimaryEffectImpl
import com.akkaserverless.javasdk.impl.valueentity.ValueEntityEffectImpl
import com.akkaserverless.javasdk.lowlevel.EventSourcedEntityFactory
import com.akkaserverless.javasdk.lowlevel.EventSourcedEntityHandler
import com.akkaserverless.javasdk.reply.ErrorReply
import com.akkaserverless.javasdk.ComponentOptions

final class EventSourcedEntityService(val factory: EventSourcedEntityFactory,
                                      override val descriptor: Descriptors.ServiceDescriptor,
                                      val anySupport: AnySupport,
                                      override val entityType: String,
                                      val snapshotEvery: Int,
                                      val entityOptions: Option[EventSourcedEntityOptions])
    extends Service {

  def this(factory: EventSourcedEntityFactory,
           descriptor: Descriptors.ServiceDescriptor,
           anySupport: AnySupport,
           entityType: String,
           snapshotEvery: Int,
           entityOptions: EventSourcedEntityOptions) =
    this(factory, descriptor, anySupport, entityType, snapshotEvery, Some(entityOptions))

  override def resolvedMethods: Option[Map[String, ResolvedServiceMethod[_, _]]] =
    factory match {
      case resolved: ResolvedEntityFactory => Some(resolved.resolvedMethods)
      case _ => None
    }

  override final val componentType = EventSourcedEntities.name
  final def withSnapshotEvery(snapshotEvery: Int): EventSourcedEntityService =
    if (snapshotEvery != this.snapshotEvery)
      new EventSourcedEntityService(this.factory,
                                    this.descriptor,
                                    this.anySupport,
                                    this.entityType,
                                    snapshotEvery,
                                    this.entityOptions)
    else
      this

  override def componentOptions: Option[ComponentOptions] = entityOptions
}

final class EventSourcedEntitiesImpl(_system: ActorSystem,
                                     _services: Map[String, EventSourcedEntityService],
                                     rootContext: Context,
                                     configuration: Configuration)
    extends EventSourcedEntities {
  import EntityExceptions._

  private final val system = _system
  private val log = Logging(system.eventStream, this.getClass)
  private final val services = _services.iterator
    .map({
      case (name, esss) =>
        if (esss.snapshotEvery < 0)
          log.warning("Snapshotting disabled for entity [{}], this is not recommended.", esss.entityType)
        // FIXME overlay configuration provided by _system
        (name, if (esss.snapshotEvery == 0) esss.withSnapshotEvery(configuration.snapshotEvery) else esss)
    })
    .toMap

  /**
   * The stream. One stream will be established per active entity.
   * Once established, the first message sent will be Init, which contains the entity ID, and,
   * if the entity has previously persisted a snapshot, it will contain that snapshot. It will
   * then send zero to many event messages, one for each event previously persisted. The entity
   * is expected to apply these to its state in a deterministic fashion. Once all the events
   * are sent, one to many commands are sent, with new commands being sent as new requests for
   * the entity come in. The entity is expected to reply to each command with exactly one reply
   * message. The entity should reply in order, and any events that the entity requests to be
   * persisted the entity should handle itself, applying them to its own state, as if they had
   * arrived as events when the event stream was being replayed on load.
   */
  override def handle(
      in: akka.stream.scaladsl.Source[EventSourcedStreamIn, akka.NotUsed]
  ): akka.stream.scaladsl.Source[EventSourcedStreamOut, akka.NotUsed] =
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
            s"Expected init message for Event Sourced Entity, but received [${other.getClass.getName}]"
          )
      }
      .recover {
        case error =>
          log.error(error, failureMessage(error))
          EventSourcedStreamOut(OutFailure(failure(error)))
      }

  private def runEntity(init: EventSourcedInit): Flow[EventSourcedStreamIn, EventSourcedStreamOut, NotUsed] = {
    val service =
      services.getOrElse(init.serviceName, throw ProtocolException(init, s"Service not found: ${init.serviceName}"))
    val handler = service.factory.create(new EventSourcedContextImpl(init.entityId))
    val thisEntityId = init.entityId

    val startingSequenceNumber = (for {
      snapshot <- init.snapshot
      any <- snapshot.snapshot
    } yield {
      val snapshotSequence = snapshot.snapshotSequence
      handler.handleSnapshot(ScalaPbAny.toJavaProto(any))
      snapshotSequence
    }).getOrElse(0L)

    Flow[EventSourcedStreamIn]
      .map(_.message)
      .scan[(Long, Option[EventSourcedStreamOut.Message])]((startingSequenceNumber, None)) {
        case (_, InEvent(event)) =>
          // Note that these only come on replay
          val context = new EventContextImpl(thisEntityId, event.sequence)
          val ev = ScalaPbAny.toJavaProto(event.payload.get) // FIXME empty?
          handler.handleEvent(ev, context)
          (event.sequence, None)
        case ((sequence, _), InCommand(command)) =>
          if (thisEntityId != command.entityId)
            throw ProtocolException(command, "Receiving entity is not the intended recipient of command")
          val cmd =
            ScalaPbAny.toJavaProto(command.payload.getOrElse(throw ProtocolException(command, "No command payload")))
          val metadata = new MetadataImpl(command.metadata.map(_.entries.toVector).getOrElse(Nil))
          val context =
            new CommandContextImpl(thisEntityId,
                                   sequence,
                                   command.name,
                                   command.id,
                                   metadata,
                                   service.anySupport,
                                   handler,
                                   service.snapshotEvery,
                                   log)

          // FIXME we'd want to somehow share this handle-command-apply-event logic to get the end effect ready for asserting in the testkit
          // FIXME a bit mixed concerns here, esp with the serialization to PbAny but it's either that or pushing this into the handler and making
          // SecondaryEffectImpl a public API (or make handler internal, which may be a good idea, also for the testkit)
          var endSequenceNumber = sequence
          val (events: Vector[ScalaPbAny], secondaryEffect: SecondaryEffectImpl, snapshot: Option[ScalaPbAny]) = try {
            val commandEffect =
              handler.handleCommand(cmd, context).asInstanceOf[EventSourcedEntityEffectImpl[Any]]
            commandEffect.primaryEffect match {
              case EmitEvents(events) =>
                var shouldSnapshot = false
                val scalaPbEvents = Vector.newBuilder[ScalaPbAny]
                events.foreach { event =>
                  val javaPbEvent = service.anySupport.encodeJava(event)
                  scalaPbEvents += ScalaPbAny.fromJavaProto(javaPbEvent)
                  endSequenceNumber = endSequenceNumber + 1
                  handler.handleEvent(javaPbEvent, new EventContextImpl(thisEntityId, sequence))
                  shouldSnapshot = shouldSnapshot || (service.snapshotEvery > 0 && endSequenceNumber % service.snapshotEvery == 0)
                }
                // FIXME currently snapshotting final state after applying all events even if trigger was mid-event stream?
                val snapshot =
                  if (shouldSnapshot) Option(ScalaPbAny.fromJavaProto(handler.currentState()))
                  else None
                (scalaPbEvents.result(),
                 commandEffect.secondaryEffect(service.anySupport.decode(handler.currentState())),
                 snapshot)
              case NoPrimaryEffect =>
                (Vector.empty, commandEffect.secondaryEffect(handler.currentState()), None)
            }
          } catch {
            case FailInvoked => new EventSourcedEntityEffectImpl[JavaPbAny]() // Ignore, error already captured
            case e: EntityException => throw e
            case NonFatal(error) =>
              throw EntityException(command, s"Unexpected failure: ${error}", Some(error))
          } finally {
            context.deactivate() // Very important!
          }

          // FIXME something more that needs to be serialized?
          val serializedSecondaryEffect = secondaryEffect match {
            case MessageReplyImpl(message, metadata, sideEffects) =>
              MessageReplyImpl(service.anySupport.encodeJava(message), metadata, sideEffects)
            case other => other
          }

          val clientAction =
            context.replyToClientAction(serializedSecondaryEffect,
                                        allowNoReply = false,
                                        restartOnFailure = events.nonEmpty)

          serializedSecondaryEffect match {
            case error: ErrorReplyImpl[_] =>
              log.error("Fail invoked for command [{}] for entity [{}]: {}",
                        command.name,
                        thisEntityId,
                        error.description)
              (endSequenceNumber,
               Some(
                 OutReply(
                   EventSourcedReply(
                     commandId = command.id,
                     clientAction = clientAction
                   )
                 )
               ))

            case ok =>
              (endSequenceNumber,
               Some(
                 OutReply(
                   EventSourcedReply(
                     command.id,
                     clientAction,
                     context.sideEffects ++ EffectSupport.sideEffectsFrom(serializedSecondaryEffect),
                     events,
                     snapshot
                   )
                 )
               ))
          }
        case (_, InInit(i)) =>
          throw ProtocolException(init, "Entity already inited")
        case (_, InEmpty) =>
          throw ProtocolException(init, "Received empty/unknown message")
      }
      .collect {
        case (_, Some(message)) => EventSourcedStreamOut(message)
      }
  }

  trait AbstractContext extends EventSourcedContext {
    override def serviceCallFactory(): ServiceCallFactory = rootContext.serviceCallFactory()
  }

  class CommandContextImpl(override val entityId: String,
                           override val sequenceNumber: Long,
                           override val commandName: String,
                           override val commandId: Long,
                           override val metadata: Metadata,
                           val anySupport: AnySupport,
                           val handler: EventSourcedEntityHandler,
                           val snapshotEvery: Int,
                           val log: LoggingAdapter)
      extends CommandContext
      with AbstractContext
      with AbstractClientActionContext
      with AbstractSideEffectContext
      with ActivatableContext {

    final var performSnapshot: Boolean = false

    override protected def logError(message: String): Unit =
      log.error("Fail invoked for command [{}] for entity [{}]: {}", commandName, entityId, message)
  }

  class EventSourcedContextImpl(override final val entityId: String) extends EventSourcedContext with AbstractContext
  class EventContextImpl(entityId: String, override final val sequenceNumber: Long)
      extends EventSourcedContextImpl(entityId)
      with EventContext
}
