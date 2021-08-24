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

import com.akkaserverless.javasdk.valueentity.CommandContext;
import com.akkaserverless.javasdk.valueentity.CommandHandler;
import com.akkaserverless.javasdk.valueentity.ValueEntityBase;
import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessageV3;

/**
 * Low level interface for handling commands on a value based entity.
 *
 * <p>The concrete <code>ValueEntityHandler</code> is generated for the specific entities defined in
 * Protobuf.
 */
public interface ValueEntityHandler {

  /**
   * Handle the given command.
   *
   * @param command The command to handle.
   * @param context The command context.
   * @return The reply to the command, if the command isn't being forwarded elsewhere.
   */
  ValueEntityBase.Effect<? extends Object> handleCommand(
      Any command, Any state, CommandContext<Any> context) throws Throwable;

  com.google.protobuf.any.Any emptyState();
}
