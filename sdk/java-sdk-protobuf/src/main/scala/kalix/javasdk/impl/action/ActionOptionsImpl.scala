/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.action

import kalix.javasdk.action.ActionOptions

import java.util

private[kalix] final case class ActionOptionsImpl(override val forwardHeaders: java.util.Set[String])
    extends ActionOptions {
  def withForwardHeaders(headers: util.Set[String]): ActionOptions = copy(forwardHeaders = headers)
}
