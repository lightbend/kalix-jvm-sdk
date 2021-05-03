/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk.replicatedentity;

import com.akkaserverless.javasdk.EntityContext;

import java.util.Optional;

/** Root context for all Replicated Entity contexts. */
public interface ReplicatedEntityContext extends EntityContext {
  /**
   * The current Replicated Data object, if it's been created.
   *
   * @param dataClass The type of the Replicated Data that is expected.
   * @return The current Replicated Data, or empty if none has been created yet.
   * @throws IllegalStateException If the current Replicated Data does not match the passed in
   *     <code>dataClass</code> type.
   */
  <T extends ReplicatedData> Optional<T> state(Class<T> dataClass) throws IllegalStateException;

  /**
   * Get the current write consistency setting for replication of the replicated entity state.
   *
   * @return the current write consistency
   */
  WriteConsistency getWriteConsistency();

  /**
   * Set the write consistency setting for replication of the replicated entity state.
   *
   * @param writeConsistency the new write consistency to use
   */
  void setWriteConsistency(WriteConsistency writeConsistency);
}
