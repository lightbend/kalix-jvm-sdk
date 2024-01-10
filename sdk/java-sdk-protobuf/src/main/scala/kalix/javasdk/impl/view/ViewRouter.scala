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

package kalix.javasdk.impl.view

import kalix.javasdk.view.{ UpdateContext, View }

import java.util.Optional

abstract class ViewUpdateRouter {
  def _internalHandleUpdate(state: Option[Any], event: Any, context: UpdateContext): View.UpdateEffect[_]
}

abstract class ViewRouter[S, V <: View[S]](protected val view: V) extends ViewUpdateRouter {

  /** INTERNAL API */
  override final def _internalHandleUpdate(
      state: Option[Any],
      event: Any,
      context: UpdateContext): View.UpdateEffect[_] = {
    val stateOrEmpty: S = state match {
      case Some(preExisting) => preExisting.asInstanceOf[S]
      case None              => view.emptyState()
    }
    try {
      view._internalSetUpdateContext(Optional.of(context))
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

  def handleUpdate(commandName: String, state: S, event: Any): View.UpdateEffect[S]

}

abstract class ViewMultiTableRouter extends ViewUpdateRouter {

  /** INTERNAL API */
  override final def _internalHandleUpdate(
      state: Option[Any],
      event: Any,
      context: UpdateContext): View.UpdateEffect[_] = {
    viewRouter(context.eventName())._internalHandleUpdate(state, event, context)
  }

  def viewRouter(eventName: String): ViewRouter[_, _]

}
