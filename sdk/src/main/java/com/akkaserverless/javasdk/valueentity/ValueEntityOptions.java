/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.valueentity;

import com.akkaserverless.javasdk.PassivationStrategy;
import com.akkaserverless.javasdk.impl.valueentity.ValueEntityOptionsImpl;

/** Root entity options for all value based entities. */
public interface ValueEntityOptions extends com.akkaserverless.javasdk.EntityOptions {

  ValueEntityOptions withPassivationStrategy(PassivationStrategy strategy);

  /**
   * Create a default entity option for a value based entity.
   *
   * @return the entity option
   */
  static ValueEntityOptions defaults() {
    return new ValueEntityOptionsImpl(PassivationStrategy.defaultTimeout());
  }
}
