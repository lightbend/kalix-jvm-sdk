/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.replicatedentity

import kalix.scalasdk
import kalix.scalasdk.EntityOptions
import kalix.scalasdk.PassivationStrategy

trait ReplicatedEntityOptions extends EntityOptions {

  def passivationStrategy: PassivationStrategy

  def withPassivationStrategy(strategy: PassivationStrategy): ReplicatedEntityOptions

  override def withForwardHeaders(headers: Set[String]): ReplicatedEntityOptions

  /**
   * Get the current write consistency setting for replication of the replicated entity state.
   *
   * @return
   *   the write consistency setting for a replicated entity
   */
  def writeConsistency: WriteConsistency

  /**
   * Set the write consistency setting for replication of the replicated entity state.
   *
   * @param writeConsistency
   *   write consistency to use
   * @return
   *   new replicated entity options with write consistency setting
   */
  def withWriteConsistency(writeConsistency: WriteConsistency): ReplicatedEntityOptions
}

object ReplicatedEntityOptions {
  val defaults: ReplicatedEntityOptions =
    ReplicatedEntityOptionsImpl(PassivationStrategy.defaultTimeout, Set.empty, WriteConsistency.Local)

  private[kalix] final case class ReplicatedEntityOptionsImpl(
      passivationStrategy: PassivationStrategy,
      forwardHeaders: Set[String],
      writeConsistency: WriteConsistency)
      extends ReplicatedEntityOptions {

    override def withForwardHeaders(headers: Set[String]): ReplicatedEntityOptions =
      copy(forwardHeaders = headers)

    def withPassivationStrategy(passivationStrategy: scalasdk.PassivationStrategy): ReplicatedEntityOptions =
      copy(passivationStrategy = passivationStrategy)

    /**
     * Set the write consistency setting for replication of the replicated entity state.
     *
     * @param writeConsistency
     *   write consistency to use
     * @return
     *   new replicated entity options with write consistency setting
     */
    override def withWriteConsistency(writeConsistency: WriteConsistency): ReplicatedEntityOptions =
      copy(writeConsistency = writeConsistency)
  }
}
