/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk;

import kalix.javasdk.impl.Timeout;

import java.time.Duration;

/** A passivation strategy. */
public interface PassivationStrategy {

  /**
   * Create a passivation strategy that passivates the entity after the default duration (30
   * seconds) of inactivity.
   *
   * @return the passivation strategy
   */
  static PassivationStrategy defaultTimeout() {
    return new Timeout();
  }

  /**
   * Create a passivation strategy that passivates the entity after a given duration of inactivity.
   *
   * @param duration of inactivity after which the passivation should occur.
   * @return the passivation strategy
   */
  static PassivationStrategy timeout(Duration duration) {
    return new Timeout(duration);
  }
}
