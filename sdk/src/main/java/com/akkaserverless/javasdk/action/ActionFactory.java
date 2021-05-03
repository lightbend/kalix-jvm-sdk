/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk.action;

/**
 * Low level interface to implement {@link Action} components.
 *
 * <p>Generally, this should not be needed, instead, a class annotated with the {@link Action}.
 */
public interface ActionFactory {
  ActionHandler create(ActionCreationContext context);
}
