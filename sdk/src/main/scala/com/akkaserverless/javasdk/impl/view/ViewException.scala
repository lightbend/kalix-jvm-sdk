/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl.view

import com.akkaserverless.javasdk.view.UpdateHandlerContext

/**
 * INTERNAL API
 */
private[impl] final case class ViewException(viewId: String, commandName: String, message: String)
    extends RuntimeException(message)

/**
 * INTERNAL API
 */
private[impl] object ViewException {
  def apply(message: String): ViewException =
    ViewException(viewId = "", commandName = "", message)

  def apply(context: UpdateHandlerContext, message: String): ViewException =
    ViewException(context.viewId, context.commandName, message)

}
