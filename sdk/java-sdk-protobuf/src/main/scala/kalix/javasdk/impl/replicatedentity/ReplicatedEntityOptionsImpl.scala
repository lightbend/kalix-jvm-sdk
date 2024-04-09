/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.replicatedentity

import kalix.javasdk.PassivationStrategy
import kalix.javasdk.replicatedentity.{ ReplicatedEntityOptions, WriteConsistency }

import java.util.Collections
import java.util

private[impl] case class ReplicatedEntityOptionsImpl(
    override val passivationStrategy: PassivationStrategy,
    override val forwardHeaders: java.util.Set[String],
    override val writeConsistency: WriteConsistency)
    extends ReplicatedEntityOptions {

  override def withPassivationStrategy(strategy: PassivationStrategy): ReplicatedEntityOptions =
    copy(passivationStrategy = strategy)

  override def withWriteConsistency(writeConsistency: WriteConsistency): ReplicatedEntityOptions =
    copy(writeConsistency = writeConsistency)

  override def withForwardHeaders(headers: util.Set[String]): ReplicatedEntityOptions =
    copy(forwardHeaders = Collections.unmodifiableSet(new util.HashSet(headers)));
}
