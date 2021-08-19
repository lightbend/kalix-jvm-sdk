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
import akka.stream.scaladsl.{Flow, Source}
import com.akkaserverless.javasdk.ComponentOptions
import com.akkaserverless.javasdk.replicatedentity.{ReplicatedData => _, _}
import com.akkaserverless.javasdk.impl._
import com.akkaserverless.javasdk.impl.reply.ReplySupport
import com.akkaserverless.javasdk.replicatedentity.ReplicatedData
import com.akkaserverless.javasdk.{Context, Metadata, Reply, Service, ServiceCallFactory}
import com.akkaserverless.protocol.component.Failure
import com.akkaserverless.protocol.entity.Command
import com.akkaserverless.protocol.replicated_entity.ReplicatedEntityStreamIn.{Message => In}
import com.akkaserverless.protocol.replicated_entity._
import com.google.protobuf.any.{Any => ScalaPbAny}
import com.google.protobuf.{Descriptors, Any => JavaPbAny}

import java.util.{function, Optional}
import scala.jdk.CollectionConverters._
import com.akkaserverless.javasdk.impl.EntityExceptions.ProtocolException
import com.akkaserverless.javasdk.lowlevel.ReplicatedEntityHandlerFactory
import com.akkaserverless.javasdk.reply.ErrorReply
import org.slf4j.LoggerFactory

final class ReplicatedEntityStatefulService(val factory: ReplicatedEntityHandlerFactory,
                                            override val descriptor: Descriptors.ServiceDescriptor,
                                            val anySupport: AnySupport,
                                            val entityOptions: Option[ReplicatedEntityOptions])
    extends Service {

  def this(factory: ReplicatedEntityHandlerFactory,
           descriptor: Descriptors.ServiceDescriptor,
           anySupport: AnySupport,
           entityOptions: ReplicatedEntityOptions) = this(factory, descriptor, anySupport, Some(entityOptions))

  override final val componentType = ReplicatedEntities.name

  override def resolvedMethods: Option[Map[String, ResolvedServiceMethod[_, _]]] =
    factory match {
      case resolved: ResolvedEntityFactory => Some(resolved.resolvedMethods)
      case _ => None
    }

  override def componentOptions: Option[ComponentOptions] = entityOptions
}

object ReplicatedEntityImpl {
  private val log = LoggerFactory.getLogger(classOf[ReplicatedEntityImpl])
}

class ReplicatedEntityImpl(system: ActorSystem,
                           services: Map[String, ReplicatedEntityStatefulService],
                           rootContext: Context)
    extends ReplicatedEntities {
  import ReplicatedEntityImpl.log

  /**
   * After invoking handle, the first message sent will always be a ReplicatedEntityInit message, containing the entity ID, and,
   * if it exists or is available, the current state of the entity. After that, one or more commands may be sent,
   * as well as deltas as they arrive, and the entire state if either the entity is created, or the proxy wishes the
   * user function to replace its entire state.
   * The user function must respond with one reply per command in. They do not necessarily have to be sent in the same
   * order that the commands were sent, the command ID is used to correlate commands to replies.
   */
  def handle(in: Source[ReplicatedEntityStreamIn, NotUsed]): Source[ReplicatedEntityStreamOut, NotUsed] =
    in.prefixAndTail(1)
      .flatMapConcat {
        case (Seq(ReplicatedEntityStreamIn(In.Init(init), _)), source) =>
          source.via(runEntity(init))
        case (Seq(), _) =>
          // if error during recovery in proxy the stream will be completed before init
          log.warn("Replicated Entity stream closed before init.")
          Source.empty[ReplicatedEntityStreamOut]
        case (Seq(ReplicatedEntityStreamIn(other, _)), _) =>
          throw ProtocolException(
            s"Expected init message for Replicated Entity, but received [${other.getClass.getName}]"
          )
        case (Seq(other), _) =>
          throw ProtocolException(
            s"Expected ReplicatedEntityStreamIn init message for Replicated Entity, but received [${other.getClass.getName}]"
          )
      }
      .recover {
        case e =>
          // FIXME translate to failure message
          throw e
      }

  private def runEntity(
      init: ReplicatedEntityInit
  ): Flow[ReplicatedEntityStreamIn, ReplicatedEntityStreamOut, NotUsed] = {
    val service =
      services.getOrElse(init.serviceName, throw new RuntimeException(s"Service not found: ${init.serviceName}"))

    val runner = new EntityRunner(service, init.entityId, init.delta.map { delta =>
      ReplicatedEntityDeltaTransformer.create(delta, service.anySupport)
    })

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
            throw new IllegalStateException("Duplicate init event for the same entity")
          case In.Empty =>
            throw new RuntimeException("Empty or unknown in message")
        }
      }
      .recover {
        case err =>
          log.error("Unexpected error, terminating Replicated Entity.", err)
          ReplicatedEntityStreamOut(ReplicatedEntityStreamOut.Message.Failure(Failure(description = err.getMessage)))
      }
  }

  private class EntityRunner(service: ReplicatedEntityStatefulService,
                             entityId: String,
                             private var replicatedData: Option[InternalReplicatedData]) {

    private val entity = {
      val ctx = new ReplicatedEntityCreationContext with CapturingReplicatedEntityFactory with ActivatableContext
      try {
        service.factory.create(ctx)
      } finally {
        ctx.deactivate()
      }
    }
    // Doesn't make sense to verify that there's no delta here.
    // ReplicatedRegister can have its value set on creation, so there may be a delta.
    // verifyNoDelta("creation")

    private def verifyNoDelta(scope: String): Unit =
      replicatedData match {
        case Some(changed) if changed.hasDelta =>
          throw new RuntimeException(s"Replicated entity was changed during $scope, this is not allowed.")
        case _ =>
      }

    def handleDelta(delta: ReplicatedEntityDelta): Unit = {
      replicatedData match {
        case Some(existing) =>
          existing.applyDelta.applyOrElse(
            delta.delta, { noMatch: ReplicatedEntityDelta.Delta =>
              throw new IllegalStateException(
                s"Received delta ${noMatch.value.getClass}, but it doesn't match the replicated data that this entity has: ${existing.name}"
              )
            }
          )
        case None => throw new IllegalStateException("Received delta for a replicated entity before it was created.")
      }
    }

    def handleCommand(command: Command): ReplicatedEntityStreamOut = {
      val ctx = new ReplicatedEntityCommandContext(command)

      val reply: Reply[JavaPbAny] = try {
        val payload = ScalaPbAny.toJavaProto(command.payload.get)
        entity.handleCommand(payload, ctx)
      } catch {
        case FailInvoked =>
          Reply.noReply() // Optional.empty[JavaPbAny]()
      } finally {
        ctx.deactivate()
      }

      val clientAction = ctx.replyToClientAction(reply, allowNoReply = true, restartOnFailure = false)

      if (ctx.hasError && !reply.isInstanceOf[ErrorReply[_]]) {
        verifyNoDelta("failed command handling")
        ReplicatedEntityStreamOut(
          ReplicatedEntityStreamOut.Message.Reply(
            ReplicatedEntityReply(
              commandId = command.id,
              clientAction = clientAction
            )
          )
        )
      } else {
        val stateAction = ctx.createAction()
        ReplicatedEntityStreamOut(
          ReplicatedEntityStreamOut.Message.Reply(
            ReplicatedEntityReply(
              commandId = command.id,
              clientAction = clientAction,
              stateAction = stateAction,
              sideEffects = ctx.sideEffects ++ ReplySupport.effectsFrom(reply)
            )
          )
        )
      }
    }

    class ReplicatedEntityCommandContext(command: Command)
        extends CommandContext
        with AbstractReplicatedEntityContext
        with CapturingReplicatedEntityFactory
        with AbstractSideEffectContext
        with AbstractClientActionContext
        with DeletableContext
        with ActivatableContext {

      override final def commandId: Long = command.id

      override final def commandName(): String = command.name

      override val metadata: Metadata = new MetadataImpl(command.metadata.map(_.entries.toVector).getOrElse(Nil))

    }

    trait DeletableContext {
      self: ActivatableContext =>

    }

    trait AbstractReplicatedEntityContext extends ReplicatedEntityContext {
      override final def state[D <: ReplicatedData](replicatedDataType: Class[D]): Optional[D] =
        replicatedData match {
          case Some(data) if replicatedDataType.isInstance(data) =>
            Optional.of(replicatedDataType.cast(data))
          case None => Optional.empty()
          case Some(wrongType) =>
            throw new IllegalStateException(
              s"The current ${wrongType.name} Replicated Entity state doesn't match requested type of ${replicatedDataType.getSimpleName}"
            )
        }

      override final def entityId(): String = EntityRunner.this.entityId

      override def serviceCallFactory(): ServiceCallFactory = rootContext.serviceCallFactory()
    }

    trait CapturingReplicatedEntityFactory
        extends AbstractReplicatedEntityFactory
        with AbstractReplicatedEntityContext {
      self: ActivatableContext =>

      private var deleted = false

      override protected final def anySupport: AnySupport = service.anySupport

      override protected final def newData[D <: InternalReplicatedData](data: D): D = {
        checkActive()
        if (replicatedData.isDefined) {
          throw new RuntimeException("This entity already has replicated data created for it!")
        }
        replicatedData = Some(data)
        data
      }

      final def delete(): Unit = {
        checkActive()
        if (!replicatedData.isDefined) {
          throw new IllegalStateException("The entity doesn't exist and so can't be deleted")
        }
        deleted = true
      }

      final def isDeleted: Boolean = deleted

      final def createAction(): Option[ReplicatedEntityStateAction] = replicatedData match {
        case Some(c) =>
          if (deleted) {
            Some(
              ReplicatedEntityStateAction(action = ReplicatedEntityStateAction.Action.Delete(ReplicatedEntityDelete()))
            )
          } else if (c.hasDelta) {
            val delta = c.delta
            c.resetDelta()
            Some(
              ReplicatedEntityStateAction(
                action = ReplicatedEntityStateAction.Action.Update(ReplicatedEntityDelta(delta))
              )
            )
          } else {
            None
          }
        case None => None
      }
    }
  }
}
