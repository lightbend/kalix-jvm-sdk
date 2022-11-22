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

package kalix.javasdk.impl.view

import kalix.javasdk.view.{ UpdateContext, View }

import java.util.Optional

abstract class ViewRouter[V <: View](protected val view: V) {

  /** INTERNAL API */
  final def _internalHandleUpdate(state: Option[Any], event: Any, context: UpdateContext): View.UpdateEffect[_] = {
    try {
      view._internalSetUpdateContext(Optional.of(context))
      val stateOrEmpty: Any = state.getOrElse(view.emptyState())
      handleUpdate(context.eventName(), stateOrEmpty, event)
    } catch {
      case missing: UpdateHandlerNotFound =>
        throw new ViewException(
          context.viewId,
          missing.eventName,
          "No update handler found for event [" + missing.eventName + "] on " + view.getClass.toString,
          Option.empty)
    } finally {
      view._internalSetUpdateContext(Optional.empty())
    }
  }

  def handleUpdate[S](commandName: String, state: S, event: Any): View.UpdateEffect[S]

  def viewTable(commandName: String, event: Any): String

}
