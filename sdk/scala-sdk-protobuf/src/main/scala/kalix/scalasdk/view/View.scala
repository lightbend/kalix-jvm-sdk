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

package kalix.scalasdk.view

import kalix.scalasdk.impl.view.ViewUpdateEffectImpl

object View {

  /**
   * @tparam S
   *   The type of the state for this view.
   */
  trait UpdateEffect[+S]

  /**
   * An UpdateEffect is a description of what Kalix needs to do after the command is handled. You can think of it as a
   * set of instructions you are passing to Kalix. Kalix will process the instructions on your behalf and ensure that
   * any data that needs to be persisted will be persisted.
   *
   * Each Kalix component defines its own effects, which are a set of predefined operations that match the capabilities
   * of that component.
   *
   * A View UpdateEffect can either:
   *
   *   - update the view state
   *   - delete the view state
   *   - ignore the event or state change notification (and not update the view state)
   *
   * Construct the effect that is returned by the command handler. The effect describes next processing actions, such as
   * emitting events and sending a reply.
   */
  object UpdateEffect {

    sealed trait Builder[S] {

      def updateState(newState: S): View.UpdateEffect[S]

      def deleteState(): View.UpdateEffect[S]

      /** Ignore this event (and continue to process the next). */
      def ignore(): View.UpdateEffect[S]

      /**
       * Trigger an error for the event. Returning this effect is equivalent to throwing an exception from the handler
       * and will lead to retrying processing of the same event until it is handled successfully.
       *
       * @param description
       *   The description of the error.
       */
      def error(description: String): View.UpdateEffect[S]
    }

    def builder[S](): Builder[S] =
      _builder.asInstanceOf[Builder[S]]

    private[scalasdk] val _builder = new Builder[Any] {

      override def updateState(newState: Any): View.UpdateEffect[Any] =
        ViewUpdateEffectImpl.Update(newState)

      override def deleteState(): View.UpdateEffect[Any] = ViewUpdateEffectImpl.Delete

      override def ignore(): View.UpdateEffect[Any] =
        ViewUpdateEffectImpl.Ignore

      override def error(description: String): View.UpdateEffect[Any] =
        ViewUpdateEffectImpl.Error(description)
    }
  }
}

/**
 * Kalix applications follow the Command Query Responsibility Segregation (CQRS) pattern (see
 * https://developer.lightbend.com/docs/akka-guide/concepts/cqrs.html).
 *
 * Kalix' Entities represent the command side where you change the state of your model in a strictly consistent and
 * isolated manner. Kalix' Views represent the query side of your application. Views are optimized for reads and allow
 * you to query your model by fields other than the entity identifier.
 *
 * When implementing a View, you define what will be its internal state (your view model) and how you want to query it.
 * The Query string defines which fields will be indexed and how the query will be executed.
 *
 * The query is executed by the Kalix when a request is made to the View.
 *
 * Views are updated in response to Event Sourced Entity events, Value Entity state changes or messages from a Topic.
 *
 * Each incoming change is handled by a command handler. Command handlers are methods returning an
 * [[kalix.scalasdk.view.View.UpdateEffect]]. The command handler is responsible for updating the View state.
 *
 * @tparam S
 *   The type of the state for this view.
 */
abstract class View[S] {

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

  protected final def effects: View.UpdateEffect.Builder[S] =
    View.UpdateEffect.builder()

  /**
   * @return
   *   an empty state object to hand to the process method when an event for a previously unknown subject id is seen.
   */
  def emptyState: S

}
