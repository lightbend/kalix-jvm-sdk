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

import java.util.Optional;

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
  public final void _internalSetActionContext(Optional<ActionContext> context) {
    actionContext = context;
  }
}
