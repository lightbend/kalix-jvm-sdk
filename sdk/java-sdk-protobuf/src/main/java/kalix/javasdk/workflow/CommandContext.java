/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.workflow;

import kalix.javasdk.MetadataContext;

/** A value based workflow command context. */
public interface CommandContext extends WorkflowContext, MetadataContext {

  /**
   * The name of the command being executed.
   *
   * @return The name of the command.
   */
  String commandName();

  /**
   * The id of the command being executed.
   *
   * @return The id of the command.
   */
  long commandId();
}
