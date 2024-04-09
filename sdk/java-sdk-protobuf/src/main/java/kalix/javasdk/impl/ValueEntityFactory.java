/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl;

import kalix.javasdk.impl.valueentity.ValueEntityRouter;
import kalix.javasdk.valueentity.ValueEntityContext;

/**
 * Low level interface for handling commands on a value based entity.
 *
 * <p>Generally, this should not be needed, instead, a class extending a generated abstract {@link
 * kalix.javasdk.valueentity.ValueEntity} should be used.
 */
public interface ValueEntityFactory {
  /**
   * Create an entity handler for the given context.
   *
   * @param context The context.
   * @return The handler for the given context.
   */
  ValueEntityRouter<?, ?> create(ValueEntityContext context);
}
