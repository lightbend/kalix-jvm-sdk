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

package kalix.scalasdk.view

import kalix.scalasdk.impl.view.ViewUpdateEffectImpl

object View {

  /**
   * @tparam S
   *   The type of the state for this view.
   */
  trait UpdateEffect[+S]

  /**
   * Construct the effect that is returned by the command handler. The effect describes next processing actions, such as
   * emitting events and sending a reply.
   */
  object UpdateEffect {

    sealed trait Builder {

      def updateState[S](newState: S): View.UpdateEffect[S]

      def deleteState[S](): View.UpdateEffect[S]

      /** Ignore this event (and continue to process the next). */
      def ignore[S](): View.UpdateEffect[S]

      /**
       * Trigger an error for the event. Returning this effect is equivalent to throwing an exception from the handler
       * and will lead to retrying processing of the same event until it is handled successfully.
       *
       * @param description
       *   The description of the error.
       */
      def error[S](description: String): View.UpdateEffect[S]
    }

    def builder(): Builder = _builder

    private[scalasdk] val _builder = new Builder {

      override def updateState[S](newState: S): View.UpdateEffect[S] =
        ViewUpdateEffectImpl.Update(newState)

      override def deleteState[S](): View.UpdateEffect[S] =
        ViewUpdateEffectImpl.Delete

      override def ignore[S](): View.UpdateEffect[S] =
        ViewUpdateEffectImpl.Ignore

      override def error[S](description: String): View.UpdateEffect[S] =
        ViewUpdateEffectImpl.Error(description)
    }
  }
}

abstract class View {

  private var _updateContext: Option[UpdateContext] = None

  /**
   * Additional context and metadata for an update handler.
   *
   * It will throw an exception if accessed from constructor.
   */
  protected final def updateContext(): UpdateContext =
    _updateContext.getOrElse(
      throw new IllegalStateException("UpdateContext is only available when handling an update."))

  /** INTERNAL API */
  private[scalasdk] def _internalSetUpdateContext(context: Option[UpdateContext]): Unit =
    _updateContext = context

  protected final def effects: View.UpdateEffect.Builder =
    View.UpdateEffect.builder()

  /**
   * @return
   *   an empty state object or `null` to hand to the process method when an event for a previously unknown subject id
   *   is seen.
   */
  def emptyState: Any

}
