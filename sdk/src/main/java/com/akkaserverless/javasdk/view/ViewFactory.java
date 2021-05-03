/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk.view;

/**
 * Low level interface for handling messages in views.
 *
 * <p>Generally, this should not be needed, instead, a class annotated with the {@link
 * com.akkaserverless.javasdk.view.View @View} and similar annotations should be used.
 */
public interface ViewFactory {
  /**
   * Create a view handler for the given context.
   *
   * @param context The context.
   * @return The handler for the given context.
   */
  ViewUpdateHandler create(ViewContext context);
}
