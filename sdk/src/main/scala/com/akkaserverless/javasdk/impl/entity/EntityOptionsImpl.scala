/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl.entity

import com.akkaserverless.javasdk.PassivationStrategy
import com.akkaserverless.javasdk.entity.EntityOptions

private[impl] case class ValueEntityOptionsImpl(override val passivationStrategy: PassivationStrategy)
    extends EntityOptions {

  override def withPassivationStrategy(strategy: PassivationStrategy): EntityOptions =
    copy(passivationStrategy = strategy)
}
