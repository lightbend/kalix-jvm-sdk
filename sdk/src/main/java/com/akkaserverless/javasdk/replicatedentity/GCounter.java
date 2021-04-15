/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.replicatedentity;

/**
 * A Grow-only Counter.
 *
 * <p>A Grow-only Counter can be incremented, but can't be decremented.
 */
public interface GCounter extends ReplicatedData {
  /**
   * Get the current value of the counter.
   *
   * @return The current value of the counter.
   */
  long getValue();

  /**
   * Increment the counter.
   *
   * @param by The amount to increment the counter by.
   * @return The new value of the counter.
   * @throws IllegalArgumentException If <code>by</code> is negative.
   */
  long increment(long by) throws IllegalArgumentException;
}
