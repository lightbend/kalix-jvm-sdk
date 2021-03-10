/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.eventsourcedentity;

/** A snapshot context. */
public interface SnapshotContext extends EventSourcedContext {
  /**
   * The sequence number of the last event that this snapshot includes.
   *
   * @return The sequence number.
   */
  long sequenceNumber();
}
