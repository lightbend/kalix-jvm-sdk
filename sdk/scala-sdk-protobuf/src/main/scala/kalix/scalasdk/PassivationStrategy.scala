/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk

import scala.concurrent.duration.FiniteDuration

import kalix.scalasdk.impl.Timeout

/** A passivation strategy. */
object PassivationStrategy extends PassivationStrategy

/** A passivation strategy. */
trait PassivationStrategy {

  /**
   * Create a passivation strategy that passivates the entity after the default duration (30 seconds) of inactivity.
   *
   * @return
   *   the passivation strategy
   */
  def defaultTimeout: PassivationStrategy = Timeout(None)

  /**
   * Create a passivation strategy that passivates the entity after a given duration of inactivity.
   *
   * @param duration
   *   of inactivity after which the passivation should occur.
   * @return
   *   the passivation strategy
   */
  def timeout(duration: FiniteDuration): PassivationStrategy = Timeout(Some(duration))
}
