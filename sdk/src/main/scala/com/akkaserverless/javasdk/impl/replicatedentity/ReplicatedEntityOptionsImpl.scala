/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl.replicatedentity

import com.akkaserverless.javasdk.PassivationStrategy
import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityOptions

private[impl] case class ReplicatedEntityOptionsImpl(override val passivationStrategy: PassivationStrategy)
    extends ReplicatedEntityOptions {

  override def withPassivationStrategy(strategy: PassivationStrategy): ReplicatedEntityOptions =
    copy(passivationStrategy = strategy)
}
