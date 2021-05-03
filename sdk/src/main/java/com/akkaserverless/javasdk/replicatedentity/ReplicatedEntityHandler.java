/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk.replicatedentity;

import com.akkaserverless.javasdk.Reply;
import com.google.protobuf.Any;

/**
 * Low level interface for handling Replicated Entity commands.
 *
 * <p>These are instantiated by a {@link ReplicatedEntityHandlerFactory}.
 *
 * <p>Generally, this should not be used, rather, a {@link ReplicatedEntity} annotated class should
 * be used.
 */
public interface ReplicatedEntityHandler {
  /**
   * Handle the given command. During the handling of a command, a Replicated Entity may be created
   * (if not already created) and updated.
   *
   * @param command The command to handle.
   * @param context The context for the command.
   * @return A reply to the command, if any is sent.
   */
  Reply<Any> handleCommand(Any command, CommandContext context);

  /**
   * Handle the given stream command. During the handling of a command, a Replicated Entity may be
   * created (if not already created) and updated.
   *
   * @param command The command to handle.
   * @param context The context for the command.
   * @return A reply to the command, if any is sent.
   */
  Reply<Any> handleStreamedCommand(Any command, StreamedCommandContext<Any> context);
}
