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

package com.akkaserverless.javasdk.impl.replicatedentity

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.{ Logging, LoggingAdapter }
import akka.stream.scaladsl.{ Flow, Source }
import com.akkaserverless.javasdk.impl._
import com.akkaserverless.javasdk.impl.effect.{ EffectSupport, ErrorReplyImpl, MessageReplyImpl }
import com.akkaserverless.javasdk.impl.replicatedentity.ReplicatedEntityEffectImpl.DeleteEntity
import com.akkaserverless.javasdk.impl.replicatedentity.ReplicatedEntityHandler.CommandResult
import com.akkaserverless.javasdk.replicatedentity._
import com.akkaserverless.javasdk.{ ComponentOptions, Context, Metadata, Service, ServiceCallFactory }
import com.akkaserverless.protocol.entity.Command
import com.akkaserverless.protocol.replicated_entity.ReplicatedEntityStreamIn.{ Message => In }
import com.akkaserverless.protocol.replicated_entity.ReplicatedEntityStreamOut.{ Message => Out }
import com.akkaserverless.protocol.replicated_entity._
import com.google.protobuf.any.{ Any => ScalaPbAny }
import com.google.protobuf.Descriptors
import scala.util.control.NonFatal

import com.akkaserverless.javasdk.impl.ReplicatedEntityFactory

final class ReplicatedEntityService(
    val factory: ReplicatedEntityFactory,
    override val descriptor: Descriptors.ServiceDescriptor,
    val anySupport: AnySupport,
    override val entityType: String,
    val entityOptions: Option[ReplicatedEntityOptions])
    extends Service {

  def this(
      factory: ReplicatedEntityFactory,
      descriptor: Descriptors.ServiceDescriptor,
      anySupport: AnySupport,
      entityType: String,
      entityOptions: ReplicatedEntityOptions) = this(factory, descriptor, anySupport, entityType, Some(entityOptions))

  override final val componentType = ReplicatedEntities.name

  override def resolvedMethods: Option[Map[String, ResolvedServiceMethod[_, _]]] =
    factory match {
      case resolved: ResolvedEntityFactory => Some(resolved.resolvedMethods)
      case _                               => None
    }

  override def componentOptions: Option[ComponentOptions] = entityOptions
}

final class ReplicatedEntitiesImpl(
    system: ActorSystem,
    services: Map[String, ReplicatedEntityService],
    rootContext: Context)
    extends ReplicatedEntities {

  import ReplicatedEntitiesImpl._
  import EntityExceptions._

  private val log = Logging(system.eventStream, this.getClass)

  /**
   * After invoking handle, the first message sent will always be a ReplicatedEntityInit message, containing the entity
   * ID, and, if it exists or is available, the current state of the entity. After that, one or more commands may be
   * sent, as well as deltas as they arrive. The user function must respond with one reply per command in. They do not
   * necessarily have to be sent in the same order that the commands were sent, the command ID is used to correlate
   * commands to replies.
   */
  def handle(in: Source[ReplicatedEntityStreamIn, NotUsed]): Source[ReplicatedEntityStreamOut, NotUsed] =
    in.prefixAndTail(1)
      .flatMapConcat {
        case (Seq(ReplicatedEntityStreamIn(In.Init(init), _)), source) =>
          source.via(runEntity(init))
        case (Seq(), _) =>
          // if error during recovery in proxy the stream will be completed before init
          log.warning("Replicated Entity stream closed before init.")
          Source.empty[ReplicatedEntityStreamOut]
        case (Seq(ReplicatedEntityStreamIn(other, _)), _) =>
          throw ProtocolException(
            s"Expected init message for Replicated Entity, but received [${other.getClass.getName}]")
      }
      .recover { case error =>
        log.error(error, failureMessage(error))
        ReplicatedEntityStreamOut(Out.Failure(failure(error)))
      }

  private def runEntity(
      init: ReplicatedEntityInit): Flow[ReplicatedEntityStreamIn, ReplicatedEntityStreamOut, NotUsed] = {
    val service =
      services.getOrElse(init.serviceName, throw ProtocolException(init, s"Service not found: ${init.serviceName}"))

    val initialData = init.delta.map { delta =>
      ReplicatedEntityDeltaTransformer.create(delta, service.anySupport)
    }

    val runner = new EntityRunner(service, init.entityId, initialData, rootContext, system, log)

    Flow[ReplicatedEntityStreamIn]
      .mapConcat { in =>
        in.message match {
          case In.Command(command) =>
            List(runner.handleCommand(command))
          case In.Delta(delta) =>
            runner.handleDelta(delta)
            Nil
          case In.Delete(_) =>
            // ???
            Nil
          case In.Init(_) =>
            throw ProtocolException(init, "Replicated Entity received additional init message")
          case In.Empty =>
            throw ProtocolException(init, "Replicated Entity received empty or unknown message")
        }
      }
      .recover { case error =>
        log.error(error, failureMessage(error))
        ReplicatedEntityStreamOut(Out.Failure(failure(error)))
      }
  }
}

object ReplicatedEntitiesImpl {
  import EntityExceptions._

  private class EntityRunner(
      service: ReplicatedEntityService,
      entityId: String,
      initialData: Option[InternalReplicatedData],
      rootContext: Context,
      system: ActorSystem,
      log: LoggingAdapter) {

    private val handler = {
      val context = new ReplicatedEntityCreationContext(entityId, rootContext, system)
      try {
        service.factory.create(context)
      } finally {
        context.deactivate()
      }
    }

    handler._internalInitialData(initialData, service.anySupport)

    def handleDelta(delta: ReplicatedEntityDelta): Unit = {
      handler._internalApplyDelta(entityId, delta)
    }

    def handleCommand(command: Command): ReplicatedEntityStreamOut = {
      if (entityId != command.entityId)
        throw ProtocolException(command, "Entity is not the intended recipient of command")

      val context = new ReplicatedEntityCommandContext(entityId, command, rootContext, system)
      val payload = command.payload.getOrElse(throw ProtocolException(command, "No command payload"))
      val cmd = service.anySupport.decode(ScalaPbAny.toJavaProto(payload))

      val CommandResult(effect: ReplicatedEntityEffectImpl[_, _]) =
        try {
          handler._internalHandleCommand(command.name, cmd, context)
        } catch {
          case e: EntityException => throw e
          case NonFatal(error)    => throw EntityException(command, s"Unexpected failure: $error", Some(error))
        } finally {
          context.deactivate()
        }

      val serializedSecondaryEffect = effect.secondaryEffect match {
        case MessageReplyImpl(message, metadata, sideEffects) =>
          MessageReplyImpl(service.anySupport.encodeJava(message), metadata, sideEffects)
        case other => other
      }

      val clientAction =
        serializedSecondaryEffect.replyToClientAction(command.id, allowNoReply = false, restartOnFailure = false)

      serializedSecondaryEffect match {
        case error: ErrorReplyImpl[_] =>
          log.error("Fail invoked for command [{}] for entity [{}]: {}", command.name, entityId, error.description)
          if (handler._internalHasDelta)
            throw EntityException(command, s"Replicated entity was changed for a failed command, this is not allowed.")
          ReplicatedEntityStreamOut(
            ReplicatedEntityStreamOut.Message.Reply(
              ReplicatedEntityReply(commandId = command.id, clientAction = clientAction)))

        case _ => // non-error
          val stateAction: Option[ReplicatedEntityStateAction] = effect.primaryEffect match {
            case DeleteEntity =>
              Some(ReplicatedEntityStateAction(ReplicatedEntityStateAction.Action.Delete(ReplicatedEntityDelete())))
            case _ =>
              if (handler._internalHasDelta) {
                val delta = handler._internalGetAndResetDelta
                Some(
                  ReplicatedEntityStateAction(ReplicatedEntityStateAction.Action.Update(ReplicatedEntityDelta(delta))))
              } else {
                None
              }
          }
          ReplicatedEntityStreamOut(
            ReplicatedEntityStreamOut.Message.Reply(
              ReplicatedEntityReply(
                command.id,
                clientAction,
                EffectSupport.sideEffectsFrom(serializedSecondaryEffect),
                stateAction)))
      }
    }
  }

  private final class ReplicatedEntityCreationContext(
      override val entityId: String,
      rootContext: Context,
      system: ActorSystem)
      extends AbstractContext(rootContext.serviceCallFactory(), system)
      with ReplicatedEntityContext
      with ActivatableContext

  private final class ReplicatedEntityCommandContext(
      override val entityId: String,
      command: Command,
      rootContext: Context,
      system: ActorSystem)
      extends AbstractContext(rootContext.serviceCallFactory(), system)
      with CommandContext
      with ActivatableContext {

    override val commandId: Long = command.id

    override val commandName: String = command.name

    override val metadata: Metadata = new MetadataImpl(command.metadata.map(_.entries.toVector).getOrElse(Nil))

  }
}
