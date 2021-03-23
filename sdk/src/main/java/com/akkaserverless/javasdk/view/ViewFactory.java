/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.view;

import com.akkaserverless.javasdk.valueentity.CommandHandler;
import com.akkaserverless.javasdk.valueentity.ValueEntityContext;
import com.akkaserverless.javasdk.valueentity.ValueEntityHandler;

/**
 * Low level interface for handling messages in views.
 *
 * <p>Generally, this should not be needed, instead, a class annotated with the {@link
 * com.akkaserverless.javasdk.view.View @View} and similar annotations should be used.
 */
public interface ViewFactory {
  /**
   * Create an view handler for the given context.
   *
   * @param context The context.
   * @return The handler for the given context.
   */
  ViewHandler create(ViewContext context);
}
