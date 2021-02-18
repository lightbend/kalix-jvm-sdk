/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.eventsourced;

import com.akkaserverless.javasdk.EntityOptions;
import com.akkaserverless.javasdk.PassivationStrategy;
import com.akkaserverless.javasdk.impl.eventsourced.EventSourcedEntityOptionsImpl;

/** Root entity options for all event sourcing entities. */
public interface EventSourcedEntityOptions extends EntityOptions {

  EventSourcedEntityOptions withPassivationStrategy(PassivationStrategy strategy);

  /**
   * Create a default entity option for an event sourced entity.
   *
   * @return the entity option
   */
  static EventSourcedEntityOptions defaults() {
    return new EventSourcedEntityOptionsImpl(PassivationStrategy.defaultTimeout());
  }
}
