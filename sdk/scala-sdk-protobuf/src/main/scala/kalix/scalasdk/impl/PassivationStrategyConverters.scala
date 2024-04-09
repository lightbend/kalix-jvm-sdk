/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.impl

import scala.compat.java8.DurationConverters.DurationOps
import scala.compat.java8.DurationConverters.FiniteDurationops

import kalix.scalasdk
import kalix.javasdk

private[scalasdk] object PassivationStrategyConverters {

  def toJava(strategy: scalasdk.PassivationStrategy): javasdk.PassivationStrategy =
    strategy match {
      case scalasdk.impl.Timeout(Some(duration)) => javasdk.PassivationStrategy.timeout(duration.toJava)
      case scalasdk.impl.Timeout(None)           => javasdk.PassivationStrategy.defaultTimeout()
    }

  def toScala(strategy: javasdk.PassivationStrategy): scalasdk.PassivationStrategy =
    strategy match {
      case javasdk.impl.Timeout(Some(duration)) => scalasdk.PassivationStrategy.timeout(duration.toScala)
      case javasdk.impl.Timeout(None)           => scalasdk.PassivationStrategy.defaultTimeout
    }
}
