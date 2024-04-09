/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.eventsourcedentity;

import kalix.javasdk.MetadataContext;

/** An event sourced command context. */
public interface CommandContext extends EventSourcedEntityContext, MetadataContext {
  /**
   * The current sequence number of events in this entity.
   *
   * @return The current sequence number.
   */
  long sequenceNumber();

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
