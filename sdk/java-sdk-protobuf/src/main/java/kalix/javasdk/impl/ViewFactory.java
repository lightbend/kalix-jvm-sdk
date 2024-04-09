/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl;

import kalix.javasdk.impl.view.ViewUpdateRouter;
import kalix.javasdk.view.ViewCreationContext;

/**
 * Low level interface for handling messages in views.
 *
 * <p>Generally, this should not be needed, instead, a class extending a generated abstract {@link
 * kalix.javasdk.view.View} should be used.
 */
public interface ViewFactory {
  /**
   * Create a view handler for the given context.
   *
   * @param context The context.
   * @return The handler for the given context.
   */
  ViewUpdateRouter create(ViewCreationContext context);
}
