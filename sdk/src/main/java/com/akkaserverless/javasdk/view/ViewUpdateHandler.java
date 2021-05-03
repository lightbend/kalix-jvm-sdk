/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk.view;

import com.akkaserverless.javasdk.Reply;
import com.google.protobuf.Any;

/**
 * Low level interface for handling messages on views.
 *
 * <p>Generally, this should not be needed, instead, a class annotated with the {@link
 * UpdateHandler @UpdateHandler} and similar annotations should be used.
 */
public interface ViewUpdateHandler {

  /**
   * Handle the given message.
   *
   * @param message The message to handle.
   * @param context The context.
   * @return The updated state.
   */
  Reply<Any> handle(Any message, UpdateHandlerContext context);
}
