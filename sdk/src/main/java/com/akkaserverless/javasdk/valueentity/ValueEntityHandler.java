/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk.valueentity;

import com.akkaserverless.javasdk.Reply;
import com.google.protobuf.Any;

/**
 * Low level interface for handling commands on a value based entity.
 *
 * <p>Generally, this should not be needed, instead, a class annotated with the {@link
 * CommandHandler} and similar annotations should be used.
 */
public interface ValueEntityHandler {

  /**
   * Handle the given command.
   *
   * @param command The command to handle.
   * @param context The command context.
   * @return The reply to the command, if the command isn't being forwarded elsewhere.
   */
  Reply<Any> handleCommand(Any command, CommandContext<Any> context);
}
