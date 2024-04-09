/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.action

import scala.collection.immutable.Set

import kalix.scalasdk.impl.ComponentOptions

object ActionOptions {

  def defaults: ActionOptions = ActionOptionsImpl(Set.empty)

  private[kalix] final case class ActionOptionsImpl(forwardHeaders: Set[String]) extends ActionOptions {

    override def withForwardHeaders(headers: Set[String]): ActionOptions =
      copy(forwardHeaders = headers)
  }
}
trait ActionOptions extends ComponentOptions {

  /**
   * Ask Kalix to forward these headers from the incoming request as metadata headers for the incoming commands. By
   * default no headers except "X-Server-Timing" are forwarded.
   */
  override def withForwardHeaders(headers: Set[String]): ActionOptions
}
