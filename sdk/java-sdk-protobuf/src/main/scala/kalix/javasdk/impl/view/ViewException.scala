/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.view

import kalix.javasdk.view.UpdateContext

/**
 * INTERNAL API
 */
private[impl] final case class ViewException(
    viewId: String,
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

  def apply(context: UpdateContext, message: String, cause: Option[Throwable]): ViewException =
    ViewException(context.viewId, context.eventName, message, cause)

}

/**
 * INTERNAL API
 */
final case class UpdateHandlerNotFound(eventName: String) extends RuntimeException
