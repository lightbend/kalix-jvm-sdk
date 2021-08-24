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

package com.akkaserverless.javasdk.lowlevel;

import com.akkaserverless.javasdk.Reply;
import com.akkaserverless.javasdk.view.UpdateHandler;
import com.akkaserverless.javasdk.view.UpdateContext;
import com.google.protobuf.Any;

/**
 * Low level interface for handling messages on views.
 *
 * <p>Generally, this should not be needed, instead, a class annotated with the {@link
 * UpdateHandler @UpdateHandler} and similar annotations should be used.
 */
public interface ViewUpdateHandler {

  /**
   * Handle the given message.
   *
   * @param message The message to handle.
   * @param context The context.
   * @return The updated state.
   */
  Reply<Any> handle(Any message, UpdateContext context);
}
