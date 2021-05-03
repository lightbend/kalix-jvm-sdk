/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl.eventsourcedentity

import com.akkaserverless.javasdk.PassivationStrategy
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityOptions

private[impl] case class EventSourcedEntityOptionsImpl(override val passivationStrategy: PassivationStrategy)
    extends EventSourcedEntityOptions {

  override def withPassivationStrategy(strategy: PassivationStrategy): EventSourcedEntityOptions =
    copy(passivationStrategy = strategy)
}
