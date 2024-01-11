/*
 * Copyright 2024 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kalix.scalasdk.replicatedentity

import kalix.scalasdk.replicatedentity.WriteConsistency

import scala.collection.immutable.Set
import kalix.scalasdk
import kalix.scalasdk.EntityOptions
import kalix.scalasdk.PassivationStrategy

trait ReplicatedEntityOptions extends EntityOptions {
  override def withPassivationStrategy(strategy: PassivationStrategy): ReplicatedEntityOptions
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
   * @returns
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

    override def withPassivationStrategy(passivationStrategy: scalasdk.PassivationStrategy): ReplicatedEntityOptions =
      copy(passivationStrategy = passivationStrategy)

    /**
     * Set the write consistency setting for replication of the replicated entity state.
     *
     * @param writeConsistency
     *   write consistency to use
     * @returns
     *   new replicated entity options with write consistency setting
     */
    override def withWriteConsistency(writeConsistency: WriteConsistency): ReplicatedEntityOptions =
      copy(writeConsistency = writeConsistency)
  }
}
