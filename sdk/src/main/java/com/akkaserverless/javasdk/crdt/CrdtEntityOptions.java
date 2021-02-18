/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.crdt;

import com.akkaserverless.javasdk.EntityOptions;
import com.akkaserverless.javasdk.PassivationStrategy;
import com.akkaserverless.javasdk.impl.crdt.CrdtEntityOptionsImpl;

/** Root entity options for all CRDT. */
public interface CrdtEntityOptions extends EntityOptions {

  CrdtEntityOptions withPassivationStrategy(PassivationStrategy strategy);

  /**
   * Create a default CRDT entity option.
   *
   * @return the entity option
   */
  static CrdtEntityOptions defaults() {
    return new CrdtEntityOptionsImpl(PassivationStrategy.defaultTimeout());
  }
}
