/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl;

import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import kalix.javasdk.impl.eventsourcedentity.EventSourcedEntityRouter;

/**
 * Low level interface for handling events and commands on an entity.
 *
 * <p>Generally, this should not be needed, instead, a class extending a generated abstract {@link
 * kalix.javasdk.eventsourcedentity.EventSourcedEntity} should be used.
 */
public interface EventSourcedEntityFactory {
  /**
   * Create an entity handler for the given context.
   *
   * @param context The context.
   * @return The handler for the given context.
   */
  EventSourcedEntityRouter<?, ?, ?> create(EventSourcedEntityContext context);
}
