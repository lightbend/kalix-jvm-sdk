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

package kalix.javasdk.replicatedentity;

import kalix.replicatedentity.ReplicatedData;

/**
 * A Vote replicated data type.
 *
 * <p>This replicated data type is used to allow all the nodes in a cluster to vote on a condition.
 */
public interface ReplicatedVote extends ReplicatedData {
  /**
   * Get the current value for this node's vote.
   *
   * @return this node's vote
   */
  boolean getSelfVote();

  /**
   * Get the number of voters participating in the vote (ie, the number of nodes in the cluster).
   *
   * @return the number of voters
   */
  int getVoters();

  /**
   * Get the number of votes for.
   *
   * @return the number of votes for
   */
  int getVotesFor();

  /**
   * Update this node's vote to the given value.
   *
   * @param vote the vote this node is contributing
   * @return a new vote, or this unchanged vote
   */
  ReplicatedVote vote(boolean vote);

  /**
   * Has at least one node voted true?
   *
   * @return {@code true} if at least one node has voted true
   */
  default boolean isAtLeastOne() {
    return getVotesFor() > 0;
  }

  /**
   * Have a majority of nodes voted true?
   *
   * @return {@code true} if more than half of the nodes have voted true
   */
  default boolean isMajority() {
    return getVotesFor() > getVoters() / 2;
  }

  /**
   * Is the vote unanimous?
   *
   * @return {@code true} if all nodes have voted true
   */
  default boolean isUnanimous() {
    return getVotesFor() == getVoters();
  }
}
