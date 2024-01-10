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

package kalix.javasdk;

import java.util.concurrent.CompletionStage;

/**
 * Represents a call to another component, performed as a forward, a side effect, or a
 * request-reply.
 *
 * <p>Not for user extension.
 *
 * @param <I> The type of message the call accepts
 * @param <O> The type of message the call returns
 */
public interface DeferredCall<I, O> {

  /** The message to pass to the call when the call is invoked. */
  I message();

  /** @return The metadata to pass with the message when the call is invoked. */
  Metadata metadata();

  /**
   * Execute this call right away and get the async result back for composition. Can be used to
   * create an async reply in an {@link kalix.javasdk.action.Action} with {@code
   * effects().asyncReply} and {@code effects().asyncEffect}
   */
  CompletionStage<O> execute();

  /** @return DeferredCall with updated metadata */
  DeferredCall<I, O> withMetadata(Metadata metadata);
}
