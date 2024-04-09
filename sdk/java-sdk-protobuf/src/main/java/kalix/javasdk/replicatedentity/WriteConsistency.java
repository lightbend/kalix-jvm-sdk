/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.replicatedentity;

/** Write consistency setting for replication of state updates for Replicated Entities. */
public enum WriteConsistency {
  /**
   * Updates will only be written to the local replica immediately, and then asynchronously
   * distributed to other replicas in the background.
   */
  LOCAL,

  /**
   * Updates will be written immediately to a majority of replicas, and then asynchronously
   * distributed to remaining replicas in the background.
   */
  MAJORITY,

  /** Updates will be written immediately to all replicas. */
  ALL
}
