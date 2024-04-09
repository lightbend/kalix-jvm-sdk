/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl

import kalix.javasdk.PassivationStrategy

import java.time.Duration

private[kalix] case class Timeout private (duration: Option[Duration]) extends PassivationStrategy {

  def this() = {
    this(None) // use the timeout from the default or customized settings
  }

  def this(duration: Duration) = {
    this(Some(duration))
  }
}
