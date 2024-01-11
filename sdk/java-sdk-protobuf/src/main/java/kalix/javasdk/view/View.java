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

package kalix.javasdk.view;

import akka.annotation.ApiMayChange;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.impl.view.ViewUpdateEffectImpl;

import java.util.Optional;

/**
 * Kalix applications follow the Command Query Responsibility Segregation (CQRS) pattern (see https://developer.lightbend.com/docs/akka-guide/concepts/cqrs.html). 
 * <p>
 * Kalix' Entities represent the command side where you change the state of your model in a strictly consistent and isolated manner.
 * Kalix' Views represent the query side of your application. Views are optimized for reads and allow you to query your model by fields other than the entity identifier.
 * <p>
 * When implementing a View, you define what will be its internal state (your view model) and how you want to query it.
 * The Query string defines which fields will be indexed and how the query will be executed.
 * <p>
 * The query is executed by the Kalix when a request is made to the View.
 * <p>
 * Views are updated in response to Event Sourced Entity events, Value Entity state changes or messages from a Topic.
 * <p>
 * Each incoming change is handled by a command handler. Command handlers are methods returning an
 * {@link UpdateEffect}. The command handler is responsible for updating the View state.
 *
 *  @param <S> The type of the state for this view.
 */
public abstract class View<S> {

  private Optional<UpdateContext> updateContext = Optional.empty();

  private Optional<S> viewState = Optional.empty();


  private boolean handlingUpdates = false;

  /**
   * Additional context and metadata for an update handler.
   *
   * <p>It will throw an exception if accessed from constructor.
   */
  protected final UpdateContext updateContext() {
    return updateContext.orElseThrow(
        () ->
            new IllegalStateException("UpdateContext is only available when handling an update."));
  }

  /** INTERNAL API */
  public void _internalSetUpdateContext(Optional<UpdateContext> context) {
    updateContext = context;
  }


  /** INTERNAL API */
  public void _internalSetViewState(S state) {
    handlingUpdates = true;
    viewState = Optional.ofNullable(state);
  }

  /**
   * Returns the view state (row) for this invocation as currently stored in Kalix.
   *
   * <p>Note that modifying the state directly will not update it in storage. To save the state, one
   * must call {{@code effects().updateState()}}.
   *
   * <p>This method can only be called when handling a command. Calling it outside a method (eg: in
   * the constructor) will raise a IllegalStateException exception.
   *
   * @throws IllegalStateException if accessed outside a handler method
   */
  @ApiMayChange
  protected final S viewState() {
    // user may call this method inside a command handler and get a null because it's legal
    // to have emptyState set to null.
    if (handlingUpdates) return viewState.orElse(null);
    else
      throw new IllegalStateException("Current state is only available when handling updates.");
  }

  protected final UpdateEffect.Builder<S> effects() {
    return ViewUpdateEffectImpl.builder();
  }

  /**
   * The default implementation of this method returns <code>null</code>. It can be overridden to
   * return a more sensible initial state.
   *
   * @return an empty state object or `null` to hand to the process method when an event for a
   *     previously unknown subject id is seen.
   */
  public S emptyState() {
    return null;
  }

  /**
   * An UpdateEffect is a description of what Kalix needs to do after the command is handled.
   * You can think of it as a set of instructions you are passing to Kalix. Kalix will process the instructions on your
   * behalf and ensure that any data that needs to be persisted will be persisted.
   * <p>
   * Each Kalix component defines its own effects, which are a set of predefined
   * operations that match the capabilities of that component.
   * <p>
   * A View UpdateEffect can either:
   * <p>
   * <ul>
   *   <li>update the view state
   *   <li>delete the view state
   *   <li>ignore the event or state change notification (and not update the view state)
   * </ul>
   * <p>
   * Construct the effect that is returned by the command handler. The effect describes next
   * processing actions, such as emitting events and sending a reply.
   *
   * @param <S> The type of the state for this entity.
   */
  public interface UpdateEffect<S> {

    interface Builder<S> {

      UpdateEffect<S> updateState(S newState);

      UpdateEffect<S> deleteState();

      /** Ignore this event (and continue to process the next). */
      UpdateEffect<S> ignore();

      /**
       * Trigger an error for the event. Returning this effect is equivalent to throwing an
       * exception from the handler and will lead to retrying processing of the same event until it
       * is handled successfully.
       *
       * @param description The description of the error.
       */
      UpdateEffect<S> error(String description);
    }
  }
}
