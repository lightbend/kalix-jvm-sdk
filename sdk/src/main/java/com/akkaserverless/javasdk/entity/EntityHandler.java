/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.entity;

import com.google.protobuf.Any;

import java.util.Optional;

/**
 * Low level interface for handling commands on a value based entity.
 *
 * <p>Generally, this should not be needed, instead, a class annotated with the {@link
 * CommandHandler} and similar annotations should be used.
 */
public interface EntityHandler {

  /**
   * Handle the given command.
   *
   * @param command The command to handle.
   * @param context The command context.
   * @return The reply to the command, if the command isn't being forwarded elsewhere.
   */
  Optional<Any> handleCommand(Any command, CommandContext<Any> context);
}
