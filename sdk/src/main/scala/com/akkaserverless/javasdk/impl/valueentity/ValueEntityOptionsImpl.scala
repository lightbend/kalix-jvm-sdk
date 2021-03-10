/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl.valueentity

import com.akkaserverless.javasdk.PassivationStrategy
import com.akkaserverless.javasdk.valueentity.ValueEntityOptions

private[impl] case class ValueEntityOptionsImpl(override val passivationStrategy: PassivationStrategy)
    extends ValueEntityOptions {

  override def withPassivationStrategy(strategy: PassivationStrategy): ValueEntityOptions =
    copy(passivationStrategy = strategy)
}
