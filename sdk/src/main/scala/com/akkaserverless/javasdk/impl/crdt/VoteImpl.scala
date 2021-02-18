/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl.crdt

import com.akkaserverless.javasdk.crdt.Vote
import com.akkaserverless.protocol.crdt.{CrdtDelta, VoteDelta}

private[crdt] final class VoteImpl extends InternalCrdt with Vote {
  override final val name = "Vote"
  private var selfVote = false
  private var voters = 1
  private var votesFor = 0
  private var selfVoteChanged = false

  override def getSelfVote: Boolean = selfVote

  override def getVoters: Int = voters

  override def getVotesFor: Int = votesFor

  override def vote(vote: Boolean): Unit =
    if (selfVote != vote) {
      if (selfVoteChanged) {
        selfVoteChanged = false
      } else {
        selfVoteChanged = true
      }
      selfVote = vote
      if (selfVote) {
        votesFor += 1
      } else {
        votesFor -= 1
      }
    }

  override def hasDelta: Boolean = selfVoteChanged

  override def delta: CrdtDelta.Delta =
    CrdtDelta.Delta.Vote(VoteDelta(selfVote))

  override def resetDelta(): Unit = selfVoteChanged = false

  override val applyDelta = {
    case CrdtDelta.Delta.Vote(VoteDelta(selfVote, votesFor, totalVoters, _)) =>
      this.selfVote = selfVote
      this.voters = totalVoters
      this.votesFor = votesFor
  }

  override def toString = s"Vote($selfVote)"
}
