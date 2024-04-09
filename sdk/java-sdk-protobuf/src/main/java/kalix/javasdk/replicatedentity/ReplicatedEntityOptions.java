/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.replicatedentity;

import kalix.javasdk.EntityOptions;
import kalix.javasdk.PassivationStrategy;
import kalix.javasdk.impl.replicatedentity.ReplicatedEntityOptionsImpl;

import java.util.Collections;

/** Root entity options for all Replicated Entities. */
public interface ReplicatedEntityOptions extends EntityOptions {

  PassivationStrategy passivationStrategy();

  ReplicatedEntityOptions withPassivationStrategy(PassivationStrategy strategy);

  /**
   * Get the current write consistency setting for replication of the replicated entity state.
   *
   * @return the write consistency setting for a replicated entity
   */
  WriteConsistency writeConsistency();

  /**
   * Set the write consistency setting for replication of the replicated entity state.
   *
   * @param writeConsistency write consistency to use
   * @returns new replicated entity options with write consistency setting
   */
  ReplicatedEntityOptions withWriteConsistency(WriteConsistency writeConsistency);

  /**
   * Create default Replicated Entity options.
   *
   * @return the entity options
   */
  static ReplicatedEntityOptions defaults() {
    return new ReplicatedEntityOptionsImpl(
        PassivationStrategy.defaultTimeout(), Collections.emptySet(), WriteConsistency.LOCAL);
  }
}
