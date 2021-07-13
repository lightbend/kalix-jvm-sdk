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

import com.akkaserverless.javasdk.eventsourcedentity.*;
import com.google.protobuf.Any;

/**
 * Low level interface for handling events and commands on an entity.
 *
 * <p>Generally, this should not be needed, instead, a class annotated with the {@link
 * EventHandler}, {@link CommandHandler} and similar annotations should be used.
 *
 * <p>FIXME this is rather an internal api, I don't think we really want to maintain this kind of
 * low level public apis?
 */
public interface EventSourcedEntityHandler {

  /**
   * Handle the given event.
   *
   * @param event The event to handle.
   * @param context The event context.
   */
  void handleEvent(Any event, EventContext context);

  /**
   * Handle the given command.
   *
   * @param command The command to handle.
   * @param context The command context.
   * @return The reply to the command, if the command isn't being forwarded elsewhere.
   */
  EventSourcedEntityBase.Effect<Any> handleCommand(Any command, CommandContext context);

  /**
   * Handle the given snapshot.
   *
   * @param snapshot The snapshot to handle.
   */
  void handleSnapshot(Any snapshot);

  /** @return The current state of the event sourced entity. */
  Any currentState();
}
