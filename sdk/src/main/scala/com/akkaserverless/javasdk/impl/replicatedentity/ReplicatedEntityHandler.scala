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

import java.util.Optional
import com.akkaserverless.javasdk.replicatedentity.{CommandContext, ReplicatedData, ReplicatedEntity}
import com.akkaserverless.javasdk.impl.{AnySupport, EntityExceptions}

object ReplicatedEntityHandler {
  final case class CommandResult(effect: ReplicatedEntity.Effect[_])

  final case class CommandHandlerNotFound(commandName: String) extends RuntimeException
}

/**
 * @tparam D the replicated data type for the entity
 *
 * <p>Not for manual user extension or interaction.
 *
 * <p>The concrete <code>ReplicatedEntityHandler</code> is generated for the specific entities defined in Protobuf.
 */
abstract class ReplicatedEntityHandler[D <: ReplicatedData, E <: ReplicatedEntity[D]](protected val entity: E) {
  import ReplicatedEntityHandler._

  private var data: D = _

  /** INTERNAL API */ // "public" api against the impl/testkit
  final def _internalInitialData(initialData: Option[InternalReplicatedData], anySupport: AnySupport): Unit =
    initialData match {
      case Some(d) => data = d.asInstanceOf[D]
      case _ =>
        val dataFactory = new ReplicatedDataFactoryImpl(anySupport)
        val emptyData = entity.emptyData(dataFactory)
        require(emptyData ne null, "Initial empty data for a replicated entity cannot be null")
        require(emptyData eq dataFactory.internalData, "Replicated data objects must be created with the given factory")
        data = emptyData
    }

  /** INTERNAL API */ // "public" api against the impl/testkit
  final def _internalData(): InternalReplicatedData = data.asInstanceOf[InternalReplicatedData]

  /** INTERNAL API */ // "public" api against the impl/testkit
  final def _internalHandleCommand(commandName: String, command: Any, context: CommandContext): CommandResult = {
    val commandEffect = try {
      entity._internalSetCommandContext(Optional.of(context))
      handleCommand(commandName, data, command, context)
        .asInstanceOf[ReplicatedEntityEffectImpl[Any]]
    } catch {
      case CommandHandlerNotFound(name) =>
        throw new EntityExceptions.EntityException(
          context.entityId(),
          context.commandId(),
          commandName,
          s"No command handler found for command [$name] on ${entity.getClass}"
        )
    } finally {
      entity._internalSetCommandContext(Optional.empty())
    }
    CommandResult(commandEffect)
  }

  protected def handleCommand(commandName: String,
                              data: D,
                              command: Any,
                              context: CommandContext): ReplicatedEntity.Effect[_]

}
