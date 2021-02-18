/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl.eventsourced

import com.akkaserverless.javasdk.PassivationStrategy
import com.akkaserverless.javasdk.eventsourced.EventSourcedEntityOptions

private[impl] case class EventSourcedEntityOptionsImpl(override val passivationStrategy: PassivationStrategy)
    extends EventSourcedEntityOptions {

  override def withPassivationStrategy(strategy: PassivationStrategy): EventSourcedEntityOptions =
    copy(passivationStrategy = strategy)
}
