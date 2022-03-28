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

package kalix.javasdk.view;

import kalix.javasdk.impl.view.ViewUpdateEffectImpl;

import java.util.Optional;

/** @param <S> The type of the state for this view. */
public abstract class View<S> {

  private Optional<UpdateContext> updateContext = Optional.empty();

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

  protected final UpdateEffect.Builder<S> effects() {
    return ViewUpdateEffectImpl.builder();
  }

  /**
   * @return an empty state object or `null` to hand to the process method when an event for a
   *     previously unknown subject id is seen.
   */
  public abstract S emptyState();

  /**
   * Construct the effect that is returned by the command handler. The effect describes next
   * processing actions, such as emitting events and sending a reply.
   *
   * @param <S> The type of the state for this entity.
   */
  public interface UpdateEffect<S> {

    interface Builder<S> {

      UpdateEffect<S> updateState(S newState);

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
