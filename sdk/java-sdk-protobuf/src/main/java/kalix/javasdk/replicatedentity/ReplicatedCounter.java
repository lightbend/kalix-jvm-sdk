/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.replicatedentity;

import kalix.replicatedentity.ReplicatedData;

/** A counter that can be incremented and decremented. */
public interface ReplicatedCounter extends ReplicatedData {
  /**
   * Get the current value of the counter.
   *
   * @return the current value of the counter
   */
  long getValue();

  /**
   * Increment the counter.
   *
   * <p>If <code>amount</code> is negative, then the counter will be decremented by that much
   * instead.
   *
   * @param amount the amount to increment the counter by
   * @return a new counter with incremented value
   */
  ReplicatedCounter increment(long amount);

  /**
   * Decrement the counter.
   *
   * <p>If <code>amount</code> is negative, then the counter will be incremented by that much
   * instead.
   *
   * @param amount the amount to decrement the counter by
   * @return a new counter with decremented value
   */
  ReplicatedCounter decrement(long amount);
}
