/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.view

import scala.collection.immutable.Set

import kalix.scalasdk.impl.ComponentOptions

object ViewOptions {

  def defaults: ViewOptions = ViewOptionsImpl(Set.empty)

  private[kalix] final case class ViewOptionsImpl(forwardHeaders: Set[String]) extends ViewOptions {

    /**
     * Ask Kalix to forward these headers from the incoming request as metadata headers for the incoming commands. By
     * default no headers except "X-Server-Timing" are forwarded.
     */
    override def withForwardHeaders(headers: Set[String]): ViewOptions =
      copy(forwardHeaders = headers)
  }
}
trait ViewOptions extends ComponentOptions
