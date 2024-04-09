/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.impl
import scala.concurrent.duration.FiniteDuration
import kalix.scalasdk.PassivationStrategy

private[kalix] final case class Timeout(duration: Option[FiniteDuration]) extends PassivationStrategy
