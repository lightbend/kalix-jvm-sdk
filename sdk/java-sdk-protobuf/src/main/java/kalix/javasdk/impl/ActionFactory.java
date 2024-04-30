/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl;

import kalix.javasdk.action.Action;
import kalix.javasdk.action.ActionCreationContext;
import kalix.javasdk.impl.action.ActionRouter;

/**
 * Low level interface to implement {@link Action} components.
 *
 * <p>Generally, this should not be needed, instead, a class extending a generated abstract {@link
 * Action} should be used.
 */
public interface ActionFactory {
  ActionRouter<?> create(ActionCreationContext context);
}
