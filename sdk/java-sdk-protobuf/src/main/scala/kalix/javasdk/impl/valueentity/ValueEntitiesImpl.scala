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

package kalix.javasdk.impl.valueentity

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Source
import io.grpc.Status
import kalix.javasdk.KalixRunner.Configuration
import kalix.javasdk.impl.ErrorHandling.BadRequestException
import kalix.javasdk.impl.telemetry.Instrumentation
import kalix.javasdk.impl.telemetry.Telemetry
import kalix.javasdk.impl.telemetry.ValueEntityCategory
import kalix.protocol.component.Failure
import org.slf4j.LoggerFactory

import scala.util.control.NonFatal

// FIXME these don't seem to be 'public API', more internals?
import com.google.protobuf.Descriptors
import kalix.javasdk.Metadata
import kalix.javasdk.impl.ValueEntityFactory
import kalix.javasdk.impl._
import kalix.javasdk.impl.effect.EffectSupport
import kalix.javasdk.impl.effect.ErrorReplyImpl
import kalix.javasdk.impl.effect.MessageReplyImpl
import kalix.javasdk.impl.valueentity.ValueEntityEffectImpl.DeleteEntity
import kalix.javasdk.impl.valueentity.ValueEntityEffectImpl.UpdateState
import kalix.javasdk.impl.valueentity.ValueEntityRouter.CommandResult
import kalix.javasdk.valueentity._
import kalix.protocol.value_entity.ValueEntityAction.Action.Delete
import kalix.protocol.value_entity.ValueEntityAction.Action.Update
import kalix.protocol.value_entity.ValueEntityStreamIn.Message.{ Command => InCommand }
import kalix.protocol.value_entity.ValueEntityStreamIn.Message.{ Empty => InEmpty }
import kalix.protocol.value_entity.ValueEntityStreamIn.Message.{ Init => InInit }
import kalix.protocol.value_entity.ValueEntityStreamOut.Message.{ Failure => OutFailure }
import kalix.protocol.value_entity.ValueEntityStreamOut.Message.{ Reply => OutReply }
import kalix.protocol.value_entity._

final class ValueEntityService(
    val factory: ValueEntityFactory,
    override val descriptor: Descriptors.ServiceDescriptor,
    override val additionalDescriptors: Array[Descriptors.FileDescriptor],
    val messageCodec: MessageCodec,
    override val serviceName: String,
    val entityOptions: Option[ValueEntityOptions])
    extends Service {

  def this(
      factory: ValueEntityFactory,
      descriptor: Descriptors.ServiceDescriptor,
      additionalDescriptors: Array[Descriptors.FileDescriptor],
      messageCodec: MessageCodec,
      entityType: String,
      entityOptions: ValueEntityOptions) =
    this(factory, descriptor, additionalDescriptors, messageCodec, entityType, Some(entityOptions))

  override def resolvedMethods: Option[Map[String, ResolvedServiceMethod[_, _]]] =
    factory match {
      case resolved: ResolvedEntityFactory => Some(resolved.resolvedMethods)
      case _                               => None
    }

  override final val componentType = ValueEntities.name

  override def componentOptions: Option[ComponentOptions] = entityOptions
}

final class ValueEntitiesImpl(
    system: ActorSystem,
    val services: Map[String, ValueEntityService],
    configuration: Configuration)
    extends ValueEntities {

  import EntityExceptions._

  private final val log = LoggerFactory.getLogger(this.getClass)

  val telemetry = Telemetry(system)
  lazy val instrumentations: Map[String, Instrumentation] = services.values.map { s =>
    (s.serviceName, telemetry.traceInstrumentation(s.serviceName, ValueEntityCategory))
  }.toMap

  private val pbCleanupDeletedValueEntityAfter =
    Some(com.google.protobuf.duration.Duration(configuration.cleanupDeletedValueEntityAfter))

  /**
   * One stream will be established per active entity. Once established, the first message sent will be Init, which
   * contains the entity ID, and, a state if the entity has previously persisted one. Once the Init message is sent, one
   * to many commands are sent to the entity. Each request coming in leads to a new command being sent to the entity.
   * The entity is expected to reply to each command with exactly one reply message. The entity should process commands
   * and reply to commands in the order they came in. When processing a command the entity can read and persist (update
   * or delete) the state.
   */
  override def handle(in: akka.stream.scaladsl.Source[ValueEntityStreamIn, akka.NotUsed])
      : akka.stream.scaladsl.Source[ValueEntityStreamOut, akka.NotUsed] =
    in.prefixAndTail(1)
      .flatMapConcat {
        case (Seq(ValueEntityStreamIn(InInit(init), _)), source) =>
          source.via(runEntity(init))
        case (Seq(), _) =>
          // if error during recovery in proxy the stream will be completed before init
          log.warn("Value Entity stream closed before init.")
          Source.empty[ValueEntityStreamOut]
        case (Seq(ValueEntityStreamIn(other, _)), _) =>
          throw ProtocolException(s"Expected init message for Value Entity, but received [${other.getClass.getName}]")
      }
      .recover { case error =>
        ErrorHandling.withCorrelationId { correlationId =>
          log.error(failureMessageForLog(error), error)
          ValueEntityStreamOut(OutFailure(Failure(description = s"Unexpected error [$correlationId]")))
        }
      }
      .async

  private def runEntity(init: ValueEntityInit): Flow[ValueEntityStreamIn, ValueEntityStreamOut, NotUsed] = {
    val service =
      services.getOrElse(init.serviceName, throw ProtocolException(init, s"Service not found: ${init.serviceName}"))
    val router =
      service.factory.create(new ValueEntityContextImpl(init.entityId, system))
    val thisEntityId = init.entityId

    init.state match {
      case Some(ValueEntityInitState(stateOpt, _)) =>
        stateOpt match {
          case Some(state) =>
            val decoded = service.messageCodec.decodeMessage(state)
            router._internalSetInitState(decoded)
          case None => // no initial state
        }
      case None =>
        throw new IllegalStateException("ValueEntityInitState is mandatory")
    }

    Flow[ValueEntityStreamIn]
      .map(_.message)
      .map {
        case InCommand(command) if thisEntityId != command.entityId =>
          throw ProtocolException(command, "Receiving Value entity is not the intended recipient of command")

        case InCommand(command) if command.payload.isEmpty =>
          throw ProtocolException(command, "No command payload for Value entity")

        case InCommand(command) =>
          val metadata = MetadataImpl.of(command.metadata.map(_.entries.toVector).getOrElse(Nil))

          if (log.isTraceEnabled) log.trace("Metadata entries [{}].", metadata.entries)
          val span = instrumentations(service.serviceName).buildSpan(service, command)

          try {
            val cmd =
              service.messageCodec.decodeMessage(
                command.payload.getOrElse(throw ProtocolException(command, "No command payload")))
            val context =
              new CommandContextImpl(thisEntityId, command.name, command.id, metadata, system)

            val CommandResult(effect: ValueEntityEffectImpl[_]) =
              try {
                router._internalHandleCommand(command.name, cmd, context)
              } catch {
                case BadRequestException(msg) =>
                  CommandResult(new ValueEntityEffectImpl[Any].error(msg, Status.Code.INVALID_ARGUMENT))
                case e: EntityException => throw e
                case NonFatal(error) =>
                  throw EntityException(command, s"Unexpected failure: $error", Some(error))
              } finally {
                context.deactivate() // Very important!
              }

            val serializedSecondaryEffect = effect.secondaryEffect match {
              case MessageReplyImpl(message, metadata, sideEffects) =>
                MessageReplyImpl(service.messageCodec.encodeJava(message), metadata, sideEffects)
              case other => other
            }

            val clientAction =
              serializedSecondaryEffect.replyToClientAction(service.messageCodec, command.id)

            serializedSecondaryEffect match {
              case error: ErrorReplyImpl[_] =>
                ValueEntityStreamOut(OutReply(ValueEntityReply(commandId = command.id, clientAction = clientAction)))

              case _ => // non-error
                val action: Option[ValueEntityAction] = effect.primaryEffect match {
                  case DeleteEntity =>
                    Some(ValueEntityAction(Delete(ValueEntityDelete(pbCleanupDeletedValueEntityAfter))))
                  case UpdateState(newState) =>
                    val newStateScalaPbAny = service.messageCodec.encodeScala(newState)
                    Some(ValueEntityAction(Update(ValueEntityUpdate(Some(newStateScalaPbAny)))))
                  case _ =>
                    None
                }

                ValueEntityStreamOut(
                  OutReply(
                    ValueEntityReply(
                      command.id,
                      clientAction,
                      EffectSupport.sideEffectsFrom(service.messageCodec, serializedSecondaryEffect),
                      action)))
            }
          } finally {
            span.foreach(_.end())
          }

        case InInit(_) =>
          throw ProtocolException(init, "Value entity already initiated")

        case InEmpty =>
          throw ProtocolException(init, "Value entity received empty/unknown message")
      }
      .recover { case error =>
        ErrorHandling.withCorrelationId { correlationId =>
          LoggerFactory.getLogger(router.entityClass).error(failureMessageForLog(error), error)
          ValueEntityStreamOut(OutFailure(Failure(description = s"Unexpected error [$correlationId]")))
        }
      }
  }

}

private[kalix] final class CommandContextImpl(
    override val entityId: String,
    override val commandName: String,
    override val commandId: Long,
    override val metadata: Metadata,
    system: ActorSystem)
    extends AbstractContext(system)
    with CommandContext
    with ActivatableContext

private[kalix] final class ValueEntityContextImpl(override val entityId: String, system: ActorSystem)
    extends AbstractContext(system)
    with ValueEntityContext
