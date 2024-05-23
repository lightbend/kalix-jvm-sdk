/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.replicatedentity

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{ Flow, Source }
import kalix.javasdk.impl._
import kalix.javasdk.impl.effect.{ EffectSupport, ErrorReplyImpl, MessageReplyImpl }
import kalix.javasdk.impl.replicatedentity.ReplicatedEntityEffectImpl.DeleteEntity
import kalix.javasdk.impl.replicatedentity.ReplicatedEntityRouter.CommandResult
import kalix.javasdk.replicatedentity._
import kalix.javasdk.Metadata
import kalix.protocol.entity.Command
import kalix.protocol.replicated_entity.ReplicatedEntityStreamIn.{ Message => In }
import kalix.protocol.replicated_entity.ReplicatedEntityStreamOut.{ Message => Out }
import kalix.protocol.replicated_entity._
import com.google.protobuf.Descriptors

import scala.util.control.NonFatal
import kalix.javasdk.impl.ReplicatedEntityFactory
import kalix.protocol.component.Failure
import org.slf4j.LoggerFactory

final class ReplicatedEntityService(
    val factory: ReplicatedEntityFactory,
    override val descriptor: Descriptors.ServiceDescriptor,
    override val additionalDescriptors: Array[Descriptors.FileDescriptor],
    val anySupport: AnySupport,
    override val serviceName: String,
    val entityOptions: Option[ReplicatedEntityOptions])
    extends Service {

  def this(
      factory: ReplicatedEntityFactory,
      descriptor: Descriptors.ServiceDescriptor,
      additionalDescriptors: Array[Descriptors.FileDescriptor],
      anySupport: AnySupport,
      entityType: String,
      entityOptions: ReplicatedEntityOptions) =
    this(factory, descriptor, additionalDescriptors, anySupport, entityType, Some(entityOptions))

  override final val componentType = ReplicatedEntities.name

  override def resolvedMethods: Option[Map[String, ResolvedServiceMethod[_, _]]] =
    factory match {
      case resolved: ResolvedEntityFactory => Some(resolved.resolvedMethods)
      case _                               => None
    }

  override def componentOptions: Option[ComponentOptions] = entityOptions
}

final class ReplicatedEntitiesImpl(system: ActorSystem, services: Map[String, ReplicatedEntityService])
    extends ReplicatedEntities {

  import ReplicatedEntitiesImpl._
  import EntityExceptions._

  private val log = LoggerFactory.getLogger(this.getClass)

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
          log.warn("Replicated Entity stream closed before init.")
          Source.empty[ReplicatedEntityStreamOut]
        case (Seq(ReplicatedEntityStreamIn(other, _)), _) =>
          throw ProtocolException(
            s"Expected init message for Replicated Entity, but received [${other.getClass.getName}]")
      }
      .recover { case error =>
        ErrorHandling.withCorrelationId { correlationId =>
          log.error(failureMessageForLog(error), error)
          ReplicatedEntityStreamOut(Out.Failure(Failure(description = s"Unexpected error [$correlationId]")))
        }
      }
      .async

  private def runEntity(
      init: ReplicatedEntityInit): Flow[ReplicatedEntityStreamIn, ReplicatedEntityStreamOut, NotUsed] = {
    val service =
      services.getOrElse(init.serviceName, throw ProtocolException(init, s"Service not found: ${init.serviceName}"))

    val initialData = init.delta.map { delta =>
      ReplicatedEntityDeltaTransformer.create(delta, service.anySupport)
    }

    val runner = new EntityRunner(service, init.entityId, initialData, system)

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
        ErrorHandling.withCorrelationId { correlationId =>
          LoggerFactory.getLogger(runner.router.entityClass).error(failureMessageForLog(error), error)
          ReplicatedEntityStreamOut(Out.Failure(Failure(description = s"Unexpected error [$correlationId]")))
        }
      }
  }
}

object ReplicatedEntitiesImpl {
  import EntityExceptions._

  private class EntityRunner(
      service: ReplicatedEntityService,
      entityId: String,
      initialData: Option[InternalReplicatedData],
      system: ActorSystem) {

    val router: ReplicatedEntityRouter[_ <: Object, _ <: Object] = {
      val context = new ReplicatedEntityCreationContext(entityId, system)
      try {
        service.factory.create(context)
      } finally {
        context.deactivate()
      }
    }

    router._internalInitialData(initialData, service.anySupport)

    def handleDelta(delta: ReplicatedEntityDelta): Unit = {
      router._internalApplyDelta(entityId, delta)
    }

    def handleCommand(command: Command): ReplicatedEntityStreamOut = {
      if (entityId != command.entityId)
        throw ProtocolException(command, "Entity is not the intended recipient of command")

      val context = new ReplicatedEntityCommandContext(entityId, command, system)
      val payload = command.payload.getOrElse(throw ProtocolException(command, "No command payload"))
      val cmd = service.anySupport.decodeMessage(payload)

      val CommandResult(effect: ReplicatedEntityEffectImpl[_, _]) =
        try {
          router._internalHandleCommand(command.name, cmd, context)
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
        serializedSecondaryEffect.replyToClientAction(service.anySupport, command.id)

      serializedSecondaryEffect match {
        case _: ErrorReplyImpl[_] =>
          if (router._internalHasDelta)
            throw EntityException(command, s"Replicated entity was changed for a failed command, this is not allowed.")
          ReplicatedEntityStreamOut(
            ReplicatedEntityStreamOut.Message.Reply(
              ReplicatedEntityReply(commandId = command.id, clientAction = clientAction)))

        case _ => // non-error
          val stateAction: Option[ReplicatedEntityStateAction] = effect.primaryEffect match {
            case DeleteEntity =>
              Some(ReplicatedEntityStateAction(ReplicatedEntityStateAction.Action.Delete(ReplicatedEntityDelete())))
            case _ =>
              if (router._internalHasDelta) {
                val delta = router._internalGetAndResetDelta
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
                EffectSupport.sideEffectsFrom(service.anySupport, serializedSecondaryEffect),
                stateAction)))
      }
    }
  }

  private final class ReplicatedEntityCreationContext(override val entityId: String, system: ActorSystem)
      extends AbstractContext(system)
      with ReplicatedEntityContext
      with ActivatableContext

  private final class ReplicatedEntityCommandContext(
      override val entityId: String,
      command: Command,
      system: ActorSystem)
      extends AbstractContext(system)
      with CommandContext
      with ActivatableContext {

    override val commandId: Long = command.id

    override val commandName: String = command.name

    override val metadata: Metadata = MetadataImpl.of(command.metadata.map(_.entries.toVector).getOrElse(Nil))

  }
}
