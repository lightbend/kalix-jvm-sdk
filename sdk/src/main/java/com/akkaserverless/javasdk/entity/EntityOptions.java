/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.entity;

import com.akkaserverless.javasdk.PassivationStrategy;
import com.akkaserverless.javasdk.impl.entity.ValueEntityOptionsImpl;

/** Root entity options for all value based entities. */
public interface EntityOptions extends com.akkaserverless.javasdk.EntityOptions {

  EntityOptions withPassivationStrategy(PassivationStrategy strategy);

  /**
   * Create a default entity option for a value based entity.
   *
   * @return the entity option
   */
  static EntityOptions defaults() {
    return new ValueEntityOptionsImpl(PassivationStrategy.defaultTimeout());
  }
}
