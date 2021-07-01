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

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.scaladsl.Flow
import com.akkaserverless.javasdk.AkkaServerlessRunner.Configuration
import com.akkaserverless.javasdk.valueentity._
import com.akkaserverless.javasdk.impl._
import com.akkaserverless.javasdk.impl.reply.ReplySupport
import com.akkaserverless.javasdk.{Context, Metadata, Reply, Service, ServiceCallFactory}
import com.akkaserverless.protocol.value_entity.ValueEntityAction.Action.{Delete, Update}
import com.akkaserverless.protocol.value_entity.ValueEntityStreamIn.Message.{
  Command => InCommand,
  Empty => InEmpty,
  Init => InInit
}
import com.akkaserverless.protocol.value_entity.ValueEntityStreamOut.Message.{Failure => OutFailure, Reply => OutReply}
import com.akkaserverless.protocol.value_entity._
import com.google.protobuf.any.{Any => ScalaPbAny}
import com.google.protobuf.{Descriptors, Any => JavaPbAny}

import java.util.Optional
import scala.compat.java8.OptionConverters._
import scala.util.control.NonFatal
import akka.stream.scaladsl.Source
import com.akkaserverless.javasdk.impl.effect.{EffectSupport, ErrorReplyImpl}
import com.akkaserverless.javasdk.impl.valueentity.ValueEntityEffectImpl.{DeleteState, UpdateState}
import com.akkaserverless.javasdk.lowlevel.ValueEntityFactory
import com.akkaserverless.javasdk.reply.ErrorReply

final class ValueEntityService(val factory: ValueEntityFactory,
                               override val descriptor: Descriptors.ServiceDescriptor,
                               val anySupport: AnySupport,
                               override val entityType: String,
                               override val entityOptions: Option[ValueEntityOptions])
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
}

final class ValueEntitiesImpl(_system: ActorSystem,
                              _services: Map[String, ValueEntityService],
                              rootContext: Context,
                              configuration: Configuration)
    extends ValueEntities {

  import EntityExceptions._

  private final val system = _system
  private final implicit val ec = system.dispatcher
  private final val services = _services.iterator.toMap
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
    val handler = service.factory.create(new EntityContextImpl(init.entityId))
    val thisEntityId = init.entityId

    val initState: Option[ScalaPbAny] = init.state match {
      case Some(ValueEntityInitState(stateOpt, _)) =>
        stateOpt match {
          case Some(state) => Some(state)
          case None => Option(handler.emptyState())
        }
      case None => throw new IllegalStateException("ValueEntityInit state is mandatory")
    }

    Flow[ValueEntityStreamIn]
      .map(_.message)
      .scan[(Option[ScalaPbAny], Option[ValueEntityStreamOut.Message])]((initState, None)) {
        case (_, InCommand(command)) if thisEntityId != command.entityId =>
          throw ProtocolException(command, "Receiving Value entity is not the intended recipient of command")

        case (_, InCommand(command)) if command.payload.isEmpty =>
          throw ProtocolException(command, "No command payload for Value entity")

        case ((state, _), InCommand(command)) =>
          val cmd = ScalaPbAny.toJavaProto(command.payload.get)
          val metadata = new MetadataImpl(command.metadata.map(_.entries.toVector).getOrElse(Nil))
          val context = new CommandContextImpl(
            thisEntityId,
            command.name,
            command.id,
            metadata,
            state,
            service.anySupport,
            log
          )
          val effect: ValueEntityEffectImpl[JavaPbAny] = try {
            handler
              .handleCommand(cmd, state.map(ScalaPbAny.toJavaProto).orNull, context)
              .asInstanceOf[ValueEntityEffectImpl[JavaPbAny]]
          } catch {
            case FailInvoked => new ValueEntityEffectImpl() //Option.empty[JavaPbAny].asJava
            case e: EntityException => throw e
            case NonFatal(error) => {
              throw EntityException(
                command,
                s"Value entity unexpected failure: ${error}",
                Some(error)
              )
            }
          } finally {
            context.deactivate() // Very important!
          }

          val clientAction =
            context.replyToClientAction(effect.secondaryEffect, allowNoReply = false, restartOnFailure = false)

          if (!context.hasError && !effect.secondaryEffect.isInstanceOf[ErrorReplyImpl[_]]) {
            val (nextState: Option[ScalaPbAny], action: Option[ValueEntityAction]) = effect.primaryEffect match {
              case DeleteState =>
                (None, Some(ValueEntityAction(Delete(ValueEntityDelete()))))
              case UpdateState(newState) =>
                val newStateScala = ScalaPbAny.fromJavaProto(newState)
                (Some(newStateScala), Some(ValueEntityAction(Update(ValueEntityUpdate(Some(newStateScala))))))
              case _ =>
                (context.currentState(), None)
            }
            (nextState,
             Some(
               OutReply(
                 ValueEntityReply(
                   command.id,
                   clientAction,
                   context.sideEffects ++ EffectSupport.sideEffectsFrom(effect.secondaryEffect),
                   action
                 )
               )
             ))
          } else {
            // rollback the state if something went wrong by using the old state
            (state,
             Some(
               OutReply(
                 ValueEntityReply(
                   commandId = command.id,
                   clientAction = clientAction
                 )
               )
             ))
          }

        case (_, InInit(_)) =>
          throw ProtocolException(init, "Value entity already inited")

        case (_, InEmpty) =>
          throw ProtocolException(init, "Value entity received empty/unknown message")
      }
      .collect {
        case (_, Some(message)) => ValueEntityStreamOut(message)
      }
  }

  trait AbstractContext extends ValueEntityContext {
    override def serviceCallFactory(): ServiceCallFactory = rootContext.serviceCallFactory()
  }

  private final class CommandContextImpl(override val entityId: String,
                                         override val commandName: String,
                                         override val commandId: Long,
                                         override val metadata: Metadata,
                                         val state: Option[ScalaPbAny],
                                         val anySupport: AnySupport,
                                         val log: LoggingAdapter)
      extends CommandContext[JavaPbAny]
      with AbstractContext
      with AbstractClientActionContext
      with AbstractSideEffectContext
      with ActivatableContext {

    private val _state: Option[ScalaPbAny] = state

    override def getState(): Optional[JavaPbAny] = {
      checkActive()
      _state.map(ScalaPbAny.toJavaProto(_)).asJava
    }

    override protected def logError(message: String): Unit =
      log.error("Fail invoked for command [{}] for Value entity [{}]: {}", commandName, entityId, message)

    def currentState(): Option[ScalaPbAny] =
      _state

  }

  private final class EntityContextImpl(override final val entityId: String)
      extends ValueEntityContext
      with AbstractContext

}
