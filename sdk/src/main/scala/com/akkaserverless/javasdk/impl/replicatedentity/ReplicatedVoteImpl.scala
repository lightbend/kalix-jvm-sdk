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

package com.akkaserverless.javasdk.impl.replicatedentity

import com.akkaserverless.javasdk.replicatedentity.ReplicatedVote
import com.akkaserverless.protocol.replicated_entity.{ReplicatedEntityDelta, VoteDelta}

private[replicatedentity] final class ReplicatedVoteImpl(
    _selfVote: Boolean = false,
    _voters: Int = 1,
    _votesFor: Int = 0
) extends ReplicatedVote
    with InternalReplicatedData {

  override final val name = "Vote"
  private var selfVote = _selfVote
  private var voters = _voters
  private var votesFor = _votesFor
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

  override def copy(): ReplicatedVoteImpl = new ReplicatedVoteImpl(selfVote, voters, votesFor)

  override def hasDelta: Boolean = selfVoteChanged

  override def delta: ReplicatedEntityDelta.Delta =
    ReplicatedEntityDelta.Delta.Vote(VoteDelta(selfVote))

  override def resetDelta(): Unit = selfVoteChanged = false

  override val applyDelta: PartialFunction[ReplicatedEntityDelta.Delta, Unit] = {
    case ReplicatedEntityDelta.Delta.Vote(VoteDelta(selfVote, votesFor, totalVoters, _)) =>
      this.selfVote = selfVote
      this.voters = totalVoters
      this.votesFor = votesFor
  }

  override def toString = s"Vote($selfVote)"
}
