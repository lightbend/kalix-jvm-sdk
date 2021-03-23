/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.view;

import com.google.protobuf.Any;

import java.util.Optional;

/**
 * Low level interface for handling messages on views.
 *
 * <p>Generally, this should not be needed, instead, a class annotated with the {@link
 * com.akkaserverless.javasdk.view.Handler @Handler} and similar annotations should be used.
 */
public interface ViewHandler {

  /**
   * Handle the given message.
   *
   * @param message The message to handle.
   * @param context The context.
   * @return The updated state.
   */
  Optional<Any> handle(Any message, HandlerContext context);
}
