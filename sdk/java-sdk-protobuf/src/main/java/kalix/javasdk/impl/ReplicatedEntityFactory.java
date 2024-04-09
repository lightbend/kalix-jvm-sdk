/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl;

import kalix.javasdk.impl.replicatedentity.ReplicatedEntityRouter;
import kalix.javasdk.replicatedentity.ReplicatedEntity;
import kalix.javasdk.replicatedentity.ReplicatedEntityContext;

/**
 * Low level interface for handling commands on a replicated entity.
 *
 * <p>Generally, this should not be needed, instead, a class extending a generated abstract {@link
 * ReplicatedEntity} should be used.
 */
public interface ReplicatedEntityFactory {
  /**
   * Create an entity handler for the given context.
   *
   * @param context The context.
   * @return The handler for the given context.
   */
  ReplicatedEntityRouter<?, ?> create(ReplicatedEntityContext context);
}
