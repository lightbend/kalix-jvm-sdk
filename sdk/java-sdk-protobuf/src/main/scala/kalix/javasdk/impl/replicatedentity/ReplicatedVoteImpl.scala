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

package kalix.javasdk.impl.replicatedentity

import kalix.javasdk.replicatedentity.ReplicatedVote
import kalix.protocol.replicated_entity.ReplicatedEntityDelta
import kalix.protocol.replicated_entity.VoteDelta
import kalix.replicatedentity.ReplicatedData

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
