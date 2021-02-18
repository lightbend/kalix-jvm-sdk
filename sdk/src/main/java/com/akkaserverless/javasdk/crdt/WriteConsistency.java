/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.crdt;

/** Write consistency setting for replication of state updates for CRDTs. */
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
