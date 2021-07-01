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

import java.util.Collection;

/**
 * A return type to allow returning forwards or failures, and attaching effects to messages.
 *
 * @param <T> The type of the message that must be returned by this call.
 */
public interface Effect<T> {
  /**
   * Whether this reply is empty: does not have a message, forward, or error.
   *
   * @return Whether the reply is empty.
   */
  boolean isEmpty();

  /**
   * The effects attached to this reply.
   *
   * @return The effects.
   */
  Collection<SideEffect> sideEffects();

  /**
   * Attach the given side effects to this reply.
   *
   * @param sideEffects The effects to attach.
   * @return A new reply with the attached effects.
   */
  Effect<T> addSideEffects(Collection<SideEffect> sideEffects);

  /**
   * Attach the given effects to this reply.
   *
   * @param effects The effects to attach.
   * @return A new reply with the attached effects.
   */
  Effect<T> addSideEffects(SideEffect... effects);
}
