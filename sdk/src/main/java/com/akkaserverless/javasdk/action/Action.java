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

package com.akkaserverless.javasdk.action;

import akka.stream.javadsl.Source;
import com.akkaserverless.javasdk.ServiceCall;
import com.akkaserverless.javasdk.SideEffect;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public abstract class Action {

  private Optional<ActionContext> actionContext = Optional.empty();

  /**
   * Additional context and metadata for a message handler.
   *
   * <p>It will throw an exception if accessed from constructor.
   */
  protected final ActionContext actionContext() {
    return actionContext.orElseThrow(
        () ->
            new IllegalStateException("ActionContext is only available when handling a message."));
  }

  /** INTERNAL API */
  public final void setActionContext(Optional<ActionContext> context) {
    actionContext = context;
  }

  public final Effect.Builder effects() {
    throw new UnsupportedOperationException("tbd");
  }

  /** @param <T> The type of the message that must be returned by this call. */
  public interface Effect<T> {
    interface Builder {
      // FIXME should it rather be "Reply"?
      <S> Effect<S> message(S message);

      <S> Effect<S> forward(ServiceCall serviceCall);

      <S> Effect<S> noReply();

      <S> Effect<S> error(String description);

      <S> Effect<S> asyncMessage(CompletionStage<S> message);

      // FIXME needed mostly for the TCK, to turn CompletionStage<Effect<S>> into an Effect<S> with an async on the "inside" not sure if it is actually useful elsewhere
      <S> Effect<S> flatten(CompletionStage<Effect<S>> futureEffect);
    }

    boolean isEmpty();

    Effect<T> addSideEffect(SideEffect sideEffect);

    Effect<T> addSideEffects(Collection<SideEffect> sideEffects);

    Collection<SideEffect> sideEffects();
  }
}
