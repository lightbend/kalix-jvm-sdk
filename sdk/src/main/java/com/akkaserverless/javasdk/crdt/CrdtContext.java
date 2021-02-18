/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.crdt;

import com.akkaserverless.javasdk.EntityContext;

import java.util.Optional;

/** Root context for all CRDT contexts. */
public interface CrdtContext extends EntityContext {
  /**
   * The current CRDT, if it's been created.
   *
   * @param crdtClass The type of the CRDT that is expected.
   * @return The current CRDT, or empty if none has been created yet.
   * @throws IllegalStateException If the current CRDT does not match the passed in <code>crdtClass
   *     </code> type.
   */
  <T extends Crdt> Optional<T> state(Class<T> crdtClass) throws IllegalStateException;

  /**
   * Get the current write consistency setting for replication of CRDT state.
   *
   * @return the current write consistency
   */
  WriteConsistency getWriteConsistency();

  /**
   * Set the write consistency setting for replication of CRDT state.
   *
   * @param writeConsistency the new write consistency to use
   */
  void setWriteConsistency(WriteConsistency writeConsistency);
}
