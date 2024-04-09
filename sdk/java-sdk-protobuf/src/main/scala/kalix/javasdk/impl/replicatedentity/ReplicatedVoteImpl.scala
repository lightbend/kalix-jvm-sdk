/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.replicatedentity

import kalix.javasdk.replicatedentity.ReplicatedVote
import kalix.protocol.replicated_entity.ReplicatedEntityDelta
import kalix.protocol.replicated_entity.VoteDelta

private[kalix] final class ReplicatedVoteImpl(
    selfVote: Boolean = false,
    votesFor: Int = 0,
    voters: Int = 1,
    selfVoteChanged: Boolean = false)
    extends ReplicatedVote
    with InternalReplicatedData {

  override type Self = ReplicatedVoteImpl
  override val name = "ReplicatedVote"

  override def getSelfVote: Boolean = selfVote

  override def getVoters: Int = voters

  override def getVotesFor: Int = votesFor

  override def vote(vote: Boolean): ReplicatedVoteImpl =
    if (selfVote == vote) {
      this
    } else {
      new ReplicatedVoteImpl(vote, votesFor + (if (vote) +1 else -1), voters, !selfVoteChanged)
    }

  override def hasDelta: Boolean = selfVoteChanged

  override def getDelta: ReplicatedEntityDelta.Delta =
    ReplicatedEntityDelta.Delta.Vote(VoteDelta(selfVote))

  override def resetDelta(): ReplicatedVoteImpl =
    new ReplicatedVoteImpl(selfVote, votesFor, voters)

  override val applyDelta: PartialFunction[ReplicatedEntityDelta.Delta, ReplicatedVoteImpl] = {
    case ReplicatedEntityDelta.Delta.Vote(VoteDelta(selfVote, votesFor, voters, _)) =>
      new ReplicatedVoteImpl(selfVote, votesFor, voters)
  }

  override def toString = s"Vote($selfVote)"

}
