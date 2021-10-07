/*
 * Copyright 2021 Lightbend Inc.
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

package com.akkaserverless.scalasdk.replicatedentity

import com.akkaserverless.replicatedentity.ReplicatedData
import com.akkaserverless.javasdk.replicatedentity.{ ReplicatedVote => JavaSdkReplicatedVote }

/**
 * A Vote replicated data type.
 *
 * This replicated data type is used to allow all the nodes in a cluster to vote on a condition.
 */
class ReplicatedVote private[scalasdk] (override val _internal: JavaSdkReplicatedVote) extends ReplicatedData {

  /**
   * Get the current value for this node's vote.
   *
   * @return
   *   this node's vote
   */
  def selfVote: Boolean = _internal.getSelfVote

  /**
   * Get the number of voters participating in the vote (ie, the number of nodes in the cluster).
   *
   * @return
   *   the number of voters
   */
  def voters: Int = _internal.getVoters

  /**
   * Get the number of votes for.
   *
   * @return
   *   the number of votes for
   */
  def votesFor: Int = _internal.getVotesFor

  /**
   * Update this node's vote to the given value.
   *
   * @param vote
   *   the vote this node is contributing
   * @return
   *   a new vote, or this unchanged vote
   */
  def vote(vote: Boolean): ReplicatedVote =
    new ReplicatedVote(_internal.vote(vote))

  /**
   * Has at least one node voted true?
   *
   * @return
   *   `true` if at least one node has voted true
   */
  def isAtLeastOne: Boolean = _internal.isAtLeastOne

  /**
   * Have a majority of nodes voted true?
   *
   * @return
   *   `true` if more than half of the nodes have voted true
   */
  def isMajority: Boolean = _internal.isMajority

  /**
   * Is the vote unanimous?
   *
   * @return
   *   `true` if all nodes have voted true
   */
  def isUnanimous: Boolean = _internal.isUnanimous
}
