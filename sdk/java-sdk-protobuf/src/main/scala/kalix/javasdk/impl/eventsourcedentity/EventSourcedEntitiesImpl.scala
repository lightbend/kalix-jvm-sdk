/*
 * Copyright 2024 Lightbend Inc.
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

package kalix.javasdk.impl.eventsourcedentity

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Source
import com.google.protobuf.Descriptors
import com.google.protobuf.any.{ Any => ScalaPbAny }
import io.grpc.Status
import kalix.javasdk.KalixRunner.Configuration
import kalix.javasdk.Metadata
import kalix.javasdk.eventsourcedentity._
import kalix.javasdk.impl.ErrorHandling.BadRequestException
import kalix.javasdk.impl._
import kalix.javasdk.impl.effect.EffectSupport
import kalix.javasdk.impl.effect.ErrorReplyImpl
import kalix.javasdk.impl.effect.MessageReplyImpl
import kalix.javasdk.impl.effect.SecondaryEffectImpl
import kalix.javasdk.impl.eventsourcedentity.EventSourcedEntityRouter.CommandResult
import kalix.javasdk.impl.telemetry.EventSourcedEntityCategory
import kalix.javasdk.impl.telemetry.Instrumentation
import kalix.javasdk.impl.telemetry.Telemetry
import kalix.protocol.component.Failure
import kalix.protocol.event_sourced_entity.EventSourcedStreamIn.Message.{ Command => InCommand }
import kalix.protocol.event_sourced_entity.EventSourcedStreamIn.Message.{ Empty => InEmpty }
import kalix.protocol.event_sourced_entity.EventSourcedStreamIn.Message.{ Event => InEvent }
import kalix.protocol.event_sourced_entity.EventSourcedStreamIn.Message.{ Init => InInit }
import kalix.protocol.event_sourced_entity.EventSourcedStreamIn.Message.{ SnapshotRequest => InSnapshotRequest }
import kalix.protocol.event_sourced_entity.EventSourcedStreamOut.Message.{ Failure => OutFailure }
import kalix.protocol.event_sourced_entity.EventSourcedStreamOut.Message.{ Reply => OutReply }
import kalix.protocol.event_sourced_entity.EventSourcedStreamOut.Message.{ SnapshotReply => OutSnapshotReply }
import kalix.protocol.event_sourced_entity._
import org.slf4j.LoggerFactory

import scala.util.control.NonFatal

final class EventSourcedEntityService(
    val factory: EventSourcedEntityFactory,
    override val descriptor: Descriptors.ServiceDescriptor,
    override val additionalDescriptors: Array[Descriptors.FileDescriptor],
    val messageCodec: MessageCodec,
    override val serviceName: String,
    val snapshotEvery: Int, // FIXME remove and only use entityOptions snapshotEvery?
    val entityOptions: Option[EventSourcedEntityOptions])
    extends Service {

  def this(
      factory: EventSourcedEntityFactory,
      descriptor: Descriptors.ServiceDescriptor,
      additionalDescriptors: Array[Descriptors.FileDescriptor],
      messageCodec: MessageCodec,
      entityType: String,
      snapshotEvery: Int,
      entityOptions: EventSourcedEntityOptions) =
    this(factory, descriptor, additionalDescriptors, messageCodec, entityType, snapshotEvery, Some(entityOptions))

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
        this.additionalDescriptors,
        this.messageCodec,
        this.serviceName,
        snapshotEvery,
        this.entityOptions)
    else
      this

  override def componentOptions: Option[ComponentOptions] = entityOptions
}

final class EventSourcedEntitiesImpl(
    system: ActorSystem,
    _services: Map[String, EventSourcedEntityService],
    configuration: Configuration)
    extends EventSourcedEntities {
  import kalix.javasdk.impl.EntityExceptions._

  private val log = LoggerFactory.getLogger(this.getClass)
  private final val services = _services.iterator.map { case (name, service) =>
    if (service.snapshotEvery < 0)
      log.warn("Snapshotting disabled for entity [{}], this is not recommended.", service.serviceName)
    // FIXME overlay configuration provided by _system
    (name, if (service.snapshotEvery == 0) service.withSnapshotEvery(configuration.snapshotEvery) else service)
  }.toMap
  val telemetry = Telemetry(system)
  lazy val instrumentations: Map[String, Instrumentation] = services.values.map { s =>
    (s.serviceName, telemetry.traceInstrumentation(s.serviceName, EventSourcedEntityCategory))
  }.toMap

  private val pbCleanupDeletedEventSourcedEntityAfter =
    Some(com.google.protobuf.duration.Duration(configuration.cleanupDeletedEventSourcedEntityAfter))

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
      : akka.stream.scaladsl.Source[EventSourcedStreamOut, akka.NotUsed] = {
    in.prefixAndTail(1)
      .flatMapConcat {
        case (Seq(EventSourcedStreamIn(InInit(init), _)), source) =>
          source.via(runEntity(init))
        case (Seq(), _) =>
          // if error during recovery in proxy the stream will be completed before init
          log.error("Event Sourced Entity stream closed before init.")
          Source.empty[EventSourcedStreamOut]
        case (Seq(EventSourcedStreamIn(other, _)), _) =>
          throw ProtocolException(
            s"Expected init message for Event Sourced Entity, but received [${other.getClass.getName}]")
      }
      .recover { case error =>
        // only "unexpected" exceptions should end up here
        ErrorHandling.withCorrelationId { correlationId =>
          log.error(failureMessageForLog(error), error)
          EventSourcedStreamOut(OutFailure(Failure(description = s"Unexpected failure [$correlationId]")))
        }
      }
  }

  private def runEntity(init: EventSourcedInit): Flow[EventSourcedStreamIn, EventSourcedStreamOut, NotUsed] = {
    val service =
      services.getOrElse(init.serviceName, throw ProtocolException(init, s"Service not found: ${init.serviceName}"))

    val router = service.factory
      .create(new EventSourcedEntityContextImpl(init.entityId))
      .asInstanceOf[EventSourcedEntityRouter[Any, Any, EventSourcedEntity[Any, Any]]]
    val thisEntityId = init.entityId

    val startingSequenceNumber = (for {
      snapshot <- init.snapshot
      any <- snapshot.snapshot
    } yield {
      val snapshotSequence = snapshot.snapshotSequence
      router._internalHandleSnapshot(service.messageCodec.decodeMessage(any))
      snapshotSequence
    }).getOrElse(0L)
    Flow[EventSourcedStreamIn]
      .map(_.message)
      .scan[(Long, Option[EventSourcedStreamOut.Message])]((startingSequenceNumber, None)) {
        case (_, InEvent(event)) =>
          // Note that these only come on replay
          val context = new EventContextImpl(thisEntityId, event.sequence)
          val ev =
            service.messageCodec
              .decodeMessage(event.payload.get)
              .asInstanceOf[AnyRef] // FIXME empty?
          router._internalHandleEvent(ev, context)
          (event.sequence, None)
        case ((sequence, _), InCommand(command)) =>
          if (thisEntityId != command.entityId)
            throw ProtocolException(command, "Receiving entity is not the intended recipient of command")
          val span = instrumentations(service.serviceName).buildSpan(service, command)
          try {
            val cmd =
              service.messageCodec.decodeMessage(
                command.payload.getOrElse(throw ProtocolException(command, "No command payload")))
            val metadata = MetadataImpl.of(command.metadata.map(_.entries.toVector).getOrElse(Nil))
            val context =
              new CommandContextImpl(thisEntityId, sequence, command.name, command.id, metadata)

            val CommandResult(
              events: Vector[Any],
              secondaryEffect: SecondaryEffectImpl,
              snapshot: Option[Any],
              endSequenceNumber,
              deleteEntity) =
              try {
                router._internalHandleCommand(
                  command.name,
                  cmd,
                  context,
                  service.snapshotEvery,
                  seqNr => new EventContextImpl(thisEntityId, seqNr))
              } catch {
                case BadRequestException(msg) =>
                  val errorReply = ErrorReplyImpl(msg, Some(Status.Code.INVALID_ARGUMENT), Vector.empty)
                  CommandResult(Vector.empty, errorReply, None, context.sequenceNumber, false)
                case e: EntityException =>
                  throw e
                case NonFatal(error) =>
                  throw EntityException(command, s"Unexpected failure: $error", Some(error))
              } finally {
                context.deactivate() // Very important!
              }

            val serializedSecondaryEffect = secondaryEffect match {
              case MessageReplyImpl(message, metadata, sideEffects) =>
                MessageReplyImpl(service.messageCodec.encodeJava(message), metadata, sideEffects)
              case other => other
            }

            val clientAction = serializedSecondaryEffect.replyToClientAction(service.messageCodec, command.id)

            serializedSecondaryEffect match {
              case _: ErrorReplyImpl[_] => // error
                (
                  endSequenceNumber,
                  Some(OutReply(EventSourcedReply(commandId = command.id, clientAction = clientAction))))
              case _ => // non-error
                val serializedEvents =
                  events.map(event => ScalaPbAny.fromJavaProto(service.messageCodec.encodeJava(event)))
                val serializedSnapshot =
                  snapshot.map(state => ScalaPbAny.fromJavaProto(service.messageCodec.encodeJava(state)))
                val delete = if (deleteEntity) pbCleanupDeletedEventSourcedEntityAfter else None
                (
                  endSequenceNumber,
                  Some(
                    OutReply(
                      EventSourcedReply(
                        command.id,
                        clientAction,
                        EffectSupport.sideEffectsFrom(service.messageCodec, serializedSecondaryEffect),
                        serializedEvents,
                        serializedSnapshot,
                        delete))))
            }
          } finally { span.foreach(_.end()) }
        case ((sequence, _), InSnapshotRequest(request)) =>
          val reply =
            EventSourcedSnapshotReply(request.requestId, Some(service.messageCodec.encodeScala(router._stateOrEmpty())))
          (sequence, Some(OutSnapshotReply(reply)))
        case (_, InInit(_)) =>
          throw ProtocolException(init, "Entity already initiated")
        case (_, InEmpty) =>
          throw ProtocolException(init, "Received empty/unknown message")
      }
      .collect { case (_, Some(message)) =>
        EventSourcedStreamOut(message)
      }
      .recover { case error =>
        // only "unexpected" exceptions should end up here
        ErrorHandling.withCorrelationId { correlationId =>
          LoggerFactory.getLogger(router.entityClass).error(failureMessageForLog(error), error)
          EventSourcedStreamOut(OutFailure(Failure(description = s"Unexpected failure [$correlationId]")))
        }
      }
      .async
  }

  private class CommandContextImpl(
      override val entityId: String,
      override val sequenceNumber: Long,
      override val commandName: String,
      override val commandId: Long,
      override val metadata: Metadata)
      extends AbstractContext(system)
      with CommandContext
      with ActivatableContext

  private class EventSourcedEntityContextImpl(override final val entityId: String)
      extends AbstractContext(system)
      with EventSourcedEntityContext
  private final class EventContextImpl(entityId: String, override val sequenceNumber: Long)
      extends EventSourcedEntityContextImpl(entityId)
      with EventContext
}
