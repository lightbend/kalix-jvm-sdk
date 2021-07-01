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

package com.akkaserverless.javasdk.valueentity;

import com.akkaserverless.javasdk.eventsourcedentity.CommandContext;
import com.akkaserverless.javasdk.impl.valueentity.ValueEntityEffectImpl;

// FIXME rename to ValueEntity when the old annotation is removed

/** @param <S> The type of the state for this entity. */
public abstract class ValueEntityBase<S> {

  /**
   * Implement by returning the initial empty state object. This object will be passed into the
   * command and event handlers, until a new state replaces it.
   *
   * <p>Also known as "zero state" or "neutral state".
   *
   * <p><code>null</code> is an allowed value.
   */
  protected abstract S emptyState();

  /**
   * Additional context and meta data for a command handler.
   *
   * <p>It will throw an exception if accessed from constructor.
   */
  protected CommandContext commandContext() {
    throw new UnsupportedOperationException("Not implemented yet"); // FIXME
  }

  protected ValueEntityEffect.Builder<S> effects() {
    return new ValueEntityEffectImpl<S>();
  }
}
