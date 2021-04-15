/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.replicatedentity;

import com.akkaserverless.javasdk.EntityOptions;
import com.akkaserverless.javasdk.PassivationStrategy;
import com.akkaserverless.javasdk.impl.replicatedentity.ReplicatedEntityOptionsImpl;

/** Root entity options for all Replicated Entities. */
public interface ReplicatedEntityOptions extends EntityOptions {

  ReplicatedEntityOptions withPassivationStrategy(PassivationStrategy strategy);

  /**
   * Create a default CRDT entity option.
   *
   * @return the entity option
   */
  static ReplicatedEntityOptions defaults() {
    return new ReplicatedEntityOptionsImpl(PassivationStrategy.defaultTimeout());
  }
}
