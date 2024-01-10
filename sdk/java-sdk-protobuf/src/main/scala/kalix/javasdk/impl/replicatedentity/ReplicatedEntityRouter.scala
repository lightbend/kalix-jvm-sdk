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

package kalix.javasdk.impl.replicatedentity

import kalix.javasdk.impl.EntityExceptions.ProtocolException
import kalix.javasdk.impl.replicatedentity.ReplicatedEntityEffectImpl.UpdateData
import java.util.Optional

import kalix.javasdk.replicatedentity.{ CommandContext, ReplicatedEntity }
import kalix.javasdk.impl.{ AnySupport, EntityExceptions }
import kalix.protocol.replicated_entity.ReplicatedEntityDelta
import kalix.replicatedentity.ReplicatedData

object ReplicatedEntityRouter {
  final case class CommandResult(effect: ReplicatedEntity.Effect[_])

  final case class CommandHandlerNotFound(commandName: String) extends RuntimeException
}

/**
 * @tparam D
 *   the replicated data type for the entity
 *
 * <p>Not for manual user extension or interaction.
 *
 * <p>The concrete <code>ReplicatedEntityRouter</code> is generated for the specific entities defined in Protobuf.
 */
abstract class ReplicatedEntityRouter[D <: ReplicatedData, E <: ReplicatedEntity[D]](protected val entity: E) {
  import ReplicatedEntityRouter._

  private var data: D = _

  private def internalData: InternalReplicatedData = data.asInstanceOf[InternalReplicatedData]

  /** INTERNAL API */
  // "public" api against the impl/testkit
  final def _internalInitialData(initialData: Option[InternalReplicatedData], anySupport: AnySupport): Unit =
    initialData match {
      case Some(d) => data = d.asInstanceOf[D]
      case _ =>
        val dataFactory = new ReplicatedDataFactoryImpl(anySupport)
        val emptyData = entity.emptyData(dataFactory)
        require(emptyData ne null, "Initial empty data for a replicated entity cannot be null")
        data = emptyData
    }

  /** INTERNAL API */
  // "public" api against the impl/testkit
  final def _internalApplyDelta(entityId: String, delta: ReplicatedEntityDelta): Unit = {
    data = internalData.applyDelta
      .applyOrElse(
        delta.delta,
        { noMatch: ReplicatedEntityDelta.Delta =>
          throw ProtocolException(
            entityId,
            s"Received delta ${noMatch.value.getClass} which doesn't match the expected replicated data type: ${internalData.name}")
        })
      .asInstanceOf[D]
  }

  /** INTERNAL API */
  // "public" api against the impl/testkit
  final def _internalHasDelta: Boolean = internalData.hasDelta

  /** INTERNAL API */
  // "public" api against the impl/testkit
  final def _internalGetAndResetDelta: ReplicatedEntityDelta.Delta = {
    val delta = internalData.getDelta
    data = internalData.resetDelta().asInstanceOf[D]
    delta
  }

  /** INTERNAL API */
  // "public" api against the impl/testkit
  final def _internalHandleCommand(commandName: String, command: Any, context: CommandContext): CommandResult = {
    val commandEffect =
      try {
        entity._internalSetCommandContext(Optional.of(context))
        // Note: replicated data objects are currently mutable, so we pass a copy to the command.
        // If the update effect is not used then we still have the old replicated data (without delta).
        handleCommand(commandName, data, command, context)
          .asInstanceOf[ReplicatedEntityEffectImpl[D, Any]]
      } catch {
        case CommandHandlerNotFound(name) =>
          throw new EntityExceptions.EntityException(
            context.entityId(),
            context.commandId(),
            commandName,
            s"No command handler found for command [$name] on ${entity.getClass}")
      } finally {
        entity._internalSetCommandContext(Optional.empty())
      }

    if (!commandEffect.hasError) {
      commandEffect.primaryEffect match {
        case UpdateData(newData) =>
          require(newData ne null, "update effect with null data is not allowed")
          data = newData.asInstanceOf[D]
        case _ =>
      }
    }

    CommandResult(commandEffect)
  }

  protected def handleCommand(
      commandName: String,
      data: D,
      command: Any,
      context: CommandContext): ReplicatedEntity.Effect[_]

  def entityClass: Class[_] = entity.getClass

}
