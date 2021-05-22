/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl.view

import com.akkaserverless.javasdk.view.UpdateHandlerContext

/**
 * INTERNAL API
 */
private[impl] final case class ViewException(viewId: String,
                                             commandName: String,
                                             message: String,
                                             cause: Option[Throwable])
    extends RuntimeException(message, cause.orNull)

/**
 * INTERNAL API
 */
private[impl] object ViewException {
  def apply(message: String): ViewException =
    ViewException(viewId = "", commandName = "", message, None)

  def apply(context: UpdateHandlerContext, message: String, cause: Option[Throwable]): ViewException =
    ViewException(context.viewId, context.commandName, message, cause)

}
