/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.view

import kalix.javasdk.view.ViewOptions

import java.util

/**
 * INTERNAL API
 */
private[kalix] final case class ViewOptionsImpl(override val forwardHeaders: java.util.Set[String])
    extends ViewOptions {
  def withForwardHeaders(headers: util.Set[String]): ViewOptions = copy(forwardHeaders = headers)
}
