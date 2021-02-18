/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl.crdt

import com.akkaserverless.javasdk.PassivationStrategy
import com.akkaserverless.javasdk.crdt.CrdtEntityOptions

private[impl] case class CrdtEntityOptionsImpl(override val passivationStrategy: PassivationStrategy)
    extends CrdtEntityOptions {

  override def withPassivationStrategy(strategy: PassivationStrategy): CrdtEntityOptions =
    copy(passivationStrategy = strategy)
}
