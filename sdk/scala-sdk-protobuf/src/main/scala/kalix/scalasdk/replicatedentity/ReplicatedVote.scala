/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.replicatedentity

import kalix.javasdk.impl.replicatedentity.ReplicatedVoteImpl
import kalix.protocol.replicated_entity.ReplicatedEntityDelta

/**
 * A Vote replicated data type.
 *
 * This replicated data type is used to allow all the nodes in a cluster to vote on a condition.
 */
class ReplicatedVote private[scalasdk] (override val delegate: ReplicatedVoteImpl) extends InternalReplicatedData {

  /**
   * Get the current value for this node's vote.
   *
   * @return
   *   this node's vote
   */
  def selfVote: Boolean = delegate.getSelfVote

  /**
   * Get the number of voters participating in the vote (ie, the number of nodes in the cluster).
   *
   * @return
   *   the number of voters
   */
  def voters: Int = delegate.getVoters

  /**
   * Get the number of votes for.
   *
   * @return
   *   the number of votes for
   */
  def votesFor: Int = delegate.getVotesFor

  /**
   * Update this node's vote to the given value.
   *
   * @param vote
   *   the vote this node is contributing
   * @return
   *   a new vote, or this unchanged vote
   */
  def vote(vote: Boolean): ReplicatedVote =
    new ReplicatedVote(delegate.vote(vote))

  /**
   * Has at least one node voted true?
   *
   * @return
   *   `true` if at least one node has voted true
   */
  def isAtLeastOne: Boolean = delegate.isAtLeastOne

  /**
   * Have a majority of nodes voted true?
   *
   * @return
   *   `true` if more than half of the nodes have voted true
   */
  def isMajority: Boolean = delegate.isMajority

  /**
   * Is the vote unanimous?
   *
   * @return
   *   `true` if all nodes have voted true
   */
  def isUnanimous: Boolean = delegate.isUnanimous

  final override type Self = ReplicatedVote

  final override def resetDelta(): ReplicatedVote =
    new ReplicatedVote(delegate.resetDelta())

  final override def applyDelta: PartialFunction[ReplicatedEntityDelta.Delta, ReplicatedVote] =
    delegate.applyDelta.andThen(new ReplicatedVote(_))
}
