/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk;

/** Options used for configuring an entity. */
public interface EntityOptions {

  /** @return the passivation strategy for an entity */
  PassivationStrategy passivationStrategy();

  /**
   * Create an entity option with the given passivation strategy.
   *
   * @param strategy to be used
   * @return the entity option
   */
  EntityOptions withPassivationStrategy(PassivationStrategy strategy);
}
