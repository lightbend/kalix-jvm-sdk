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

package com.akkaserverless.javasdk.impl.valueentity

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.event.LoggingAdapter
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Source
import com.akkaserverless.javasdk.AkkaServerlessRunner.Configuration
import com.akkaserverless.javasdk.ComponentOptions
import com.akkaserverless.javasdk.Context
import com.akkaserverless.javasdk.Metadata
import com.akkaserverless.javasdk.Service
import com.akkaserverless.javasdk.ServiceCallFactory
import com.akkaserverless.javasdk.impl._
import com.akkaserverless.javasdk.impl.effect.EffectSupport
import com.akkaserverless.javasdk.impl.effect.ErrorReplyImpl
import com.akkaserverless.javasdk.impl.effect.MessageReplyImpl
import com.akkaserverless.javasdk.impl.valueentity.ValueEntityEffectImpl.DeleteState
import com.akkaserverless.javasdk.impl.valueentity.ValueEntityEffectImpl.UpdateState
import com.akkaserverless.javasdk.impl.valueentity.ValueEntityHandler.CommandResult
import com.akkaserverless.javasdk.lowlevel.ValueEntityFactory
import com.akkaserverless.javasdk.valueentity._
import com.akkaserverless.protocol.value_entity.ValueEntityAction.Action.Delete
import com.akkaserverless.protocol.value_entity.ValueEntityAction.Action.Update
import com.akkaserverless.protocol.value_entity.ValueEntityStreamIn.Message.{Command => InCommand}
import com.akkaserverless.protocol.value_entity.ValueEntityStreamIn.Message.{Empty => InEmpty}
import com.akkaserverless.protocol.value_entity.ValueEntityStreamIn.Message.{Init => InInit}
import com.akkaserverless.protocol.value_entity.ValueEntityStreamOut.Message.{Failure => OutFailure}
import com.akkaserverless.protocol.value_entity.ValueEntityStreamOut.Message.{Reply => OutReply}
import com.akkaserverless.protocol.value_entity._
import com.google.protobuf.Descriptors
import com.google.protobuf.any.{Any => ScalaPbAny}
import com.google.protobuf.{Any => JavaPbAny}

final class ValueEntityService(val factory: ValueEntityFactory,
                               override val descriptor: Descriptors.ServiceDescriptor,
                               val anySupport: AnySupport,
                               override val entityType: String,
                               val entityOptions: Option[ValueEntityOptions])
    extends Service {

  def this(factory: ValueEntityFactory,
           descriptor: Descriptors.ServiceDescriptor,
           anySupport: AnySupport,
           entityType: String,
           entityOptions: ValueEntityOptions) =
    this(factory, descriptor, anySupport, entityType, Some(entityOptions))

  override def resolvedMethods: Option[Map[String, ResolvedServiceMethod[_, _]]] =
    factory match {
      case resolved: ResolvedEntityFactory => Some(resolved.resolvedMethods)
      case _ => None
    }

  override final val componentType = ValueEntities.name

  override def componentOptions: Option[ComponentOptions] = entityOptions
}

final class ValueEntitiesImpl(system: ActorSystem,
                              val services: Map[String, ValueEntityService],
                              rootContext: Context,
                              configuration: Configuration)
    extends ValueEntities {

  import EntityExceptions._

  private implicit val ec: ExecutionContext = system.dispatcher
  private final val log = Logging(system.eventStream, this.getClass)

  /**
   * One stream will be established per active entity.
   * Once established, the first message sent will be Init, which contains the entity ID, and,
   * a state if the entity has previously persisted one. Once the Init message is sent, one to
   * many commands are sent to the entity. Each request coming in leads to a new command being sent
   * to the entity. The entity is expected to reply to each command with exactly one reply message.
   * The entity should process commands and reply to commands in the order they came
   * in. When processing a command the entity can read and persist (update or delete) the state.
   */
  override def handle(
      in: akka.stream.scaladsl.Source[ValueEntityStreamIn, akka.NotUsed]
  ): akka.stream.scaladsl.Source[ValueEntityStreamOut, akka.NotUsed] =
    in.prefixAndTail(1)
      .flatMapConcat {
        case (Seq(ValueEntityStreamIn(InInit(init), _)), source) =>
          source.via(runEntity(init))
        case (Seq(), _) =>
          // if error during recovery in proxy the stream will be completed before init
          log.warning("Value Entity stream closed before init.")
          Source.empty[ValueEntityStreamOut]
        case (Seq(ValueEntityStreamIn(other, _)), _) =>
          throw ProtocolException(s"Expected init message for Value Entity, but received [${other.getClass.getName}]")
      }
      .recover {
        case error =>
          log.error(error, failureMessage(error))
          ValueEntityStreamOut(OutFailure(failure(error)))
      }

  private def runEntity(init: ValueEntityInit): Flow[ValueEntityStreamIn, ValueEntityStreamOut, NotUsed] = {
    val service =
      services.getOrElse(init.serviceName, throw ProtocolException(init, s"Service not found: ${init.serviceName}"))
    val handler = service.factory.create(new ValueEntityContextImpl(init.entityId))
    val thisEntityId = init.entityId

    init.state match {
      case Some(ValueEntityInitState(stateOpt, _)) =>
        stateOpt.map(service.anySupport.decode).foreach(handler._internalSetInitState)
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
          if (thisEntityId != command.entityId)
            throw ProtocolException(command, "Receiving entity is not the intended recipient of command")

          val metadata = new MetadataImpl(command.metadata.map(_.entries.toVector).getOrElse(Nil))
          val cmd =
            service.anySupport.decode(
              ScalaPbAny.toJavaProto(command.payload.getOrElse(throw ProtocolException(command, "No command payload")))
            )
          val context = new CommandContextImpl(
            thisEntityId,
            command.name,
            command.id,
            metadata,
            log
          )

          val CommandResult(effect: ValueEntityEffectImpl[_]) = try {
            handler._internalHandleCommand(command.name, cmd, context)
          } catch {
            case FailInvoked => new ValueEntityEffectImpl[JavaPbAny]() // Ignore, error already captured
            case e: EntityException => throw e
            case NonFatal(error) =>
              throw EntityException(command, s"Unexpected failure: $error", Some(error))
          } finally {
            context.deactivate() // Very important!
          }

          val serializedSecondaryEffect = effect.secondaryEffect match {
            case MessageReplyImpl(message, metadata, sideEffects) =>
              MessageReplyImpl(service.anySupport.encodeJava(message), metadata, sideEffects)
            case other => other
          }

          val clientAction =
            context.replyToClientAction(serializedSecondaryEffect, allowNoReply = false, restartOnFailure = false)

          serializedSecondaryEffect match {
            case error: ErrorReplyImpl[_] =>
              log.error("Fail invoked for command [{}] for entity [{}]: {}",
                        command.name,
                        thisEntityId,
                        error.description)
              ValueEntityStreamOut(
                OutReply(
                  ValueEntityReply(
                    commandId = command.id,
                    clientAction = clientAction
                  )
                )
              )

            case _ => // non-error
              val action: Option[ValueEntityAction] = effect.primaryEffect match {
                case DeleteState =>
                  Some(ValueEntityAction(Delete(ValueEntityDelete())))
                case UpdateState(newState) =>
                  val newStateScalaPbAny = ScalaPbAny.fromJavaProto(service.anySupport.encodeJava(newState))
                  Some(ValueEntityAction(Update(ValueEntityUpdate(Some(newStateScalaPbAny)))))
                case _ =>
                  None
              }

              ValueEntityStreamOut(
                OutReply(
                  ValueEntityReply(
                    command.id,
                    clientAction,
                    context.sideEffects ++ EffectSupport.sideEffectsFrom(serializedSecondaryEffect),
                    action
                  )
                )
              )
          }

        case InInit(_) =>
          throw ProtocolException(init, "Value entity already inited")

        case InEmpty =>
          throw ProtocolException(init, "Value entity received empty/unknown message")
      }
  }

  private trait AbstractContext extends ValueEntityContext {
    override def serviceCallFactory(): ServiceCallFactory = rootContext.serviceCallFactory()
  }

  private final class CommandContextImpl(override val entityId: String,
                                         override val commandName: String,
                                         override val commandId: Long,
                                         override val metadata: Metadata,
                                         val log: LoggingAdapter)
      extends CommandContext
      with AbstractContext
      with AbstractClientActionContext
      with AbstractSideEffectContext
      with ActivatableContext {

    override protected def logError(message: String): Unit =
      log.error("Fail invoked for command [{}] for Value entity [{}]: {}", commandName, entityId, message)

  }

  private final class ValueEntityContextImpl(override val entityId: String)
      extends ValueEntityContext
      with AbstractContext

}
