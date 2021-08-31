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

import java.util.Optional

import com.akkaserverless.javasdk.valueentity.CommandContext
import com.akkaserverless.javasdk.impl.EntityExceptions
import com.akkaserverless.javasdk.impl.effect.ErrorReplyImpl
import com.akkaserverless.javasdk.impl.valueentity.ValueEntityEffectImpl.DeleteState
import com.akkaserverless.javasdk.impl.valueentity.ValueEntityEffectImpl.UpdateState
import com.akkaserverless.javasdk.valueentity.ValueEntity

object ValueEntityHandler {
  final case class CommandResult(effect: ValueEntity.Effect[_])

  final case class CommandHandlerNotFound(commandName: String) extends RuntimeException

}

/**
 * @tparam S the type of the managed state for the entity
 * Not for manual user extension or interaction
 *
 * The concrete <code>ValueEntityHandler</code> is generated for the specific entities defined in Protobuf.
 */
abstract class ValueEntityHandler[S, E <: ValueEntity[S]](protected val entity: E) {
  import ValueEntityHandler._

  private var state: Option[S] = None

  private def stateOrEmpty(): S = state match {
    case None =>
      val emptyState = entity.emptyState()
      // FIXME null should be allowed, issue #167
      require(emptyState != null, "Entity empty state is not allowed to be null")
      state = Option(emptyState)
      emptyState
    case Some(state) => state
  }

  /** INTERNAL API */ // "public" api against the impl/testkit
  final def _internalSetInitState(s: Any): Unit = {
    state = Some(s.asInstanceOf[S])
  }

  /** INTERNAL API */ // "public" api against the impl/testkit
  final def _internalHandleCommand(commandName: String, command: Any, context: CommandContext): CommandResult = {
    val commandEffect = try {
      entity.setCommandContext(Optional.of(context))
      handleCommand(commandName, stateOrEmpty(), command, context)
        .asInstanceOf[ValueEntityEffectImpl[Any]]
    } catch {
      case CommandHandlerNotFound(name) =>
        throw new EntityExceptions.EntityException(
          context.entityId(),
          context.commandId(),
          commandName,
          s"No command handler found for command [$name] on ${entity.getClass}"
        )
    } finally {
      entity.setCommandContext(Optional.empty())
    }

    if (!commandEffect.hasError()) {
      commandEffect.primaryEffect match {
        case UpdateState(newState) => state = Some(newState.asInstanceOf[S])
        case DeleteState => state = None
        case _ =>
      }
    }

    CommandResult(commandEffect)
  }

  protected def handleCommand(commandName: String,
                              state: S,
                              command: Any,
                              context: CommandContext): ValueEntity.Effect[_]

}
