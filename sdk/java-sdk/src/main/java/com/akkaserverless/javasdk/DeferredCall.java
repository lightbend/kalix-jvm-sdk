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

package com.akkaserverless.javasdk;

import java.util.concurrent.CompletionStage;

/**
 * Represents a call to a service, performed either as a forward, or as an effect.
 *
 * <p>Not for user extension.
 *
 * @param <I> The type of message the call accepts
 * @param <O> The type of message the call returns
 */
public interface DeferredCall<I, O> {

  /**
   * The message to pass to the call when the call is invoked.
   *
   * @return The message to pass to the call
   */
  I message();

  /**
   * The metadata to pass with the message when the call is invoked.
   *
   * @return The metadata.
   */
  Metadata metadata();

  /** Execute this call right away and get the async result back for composition. */
  CompletionStage<O> execute();
}
