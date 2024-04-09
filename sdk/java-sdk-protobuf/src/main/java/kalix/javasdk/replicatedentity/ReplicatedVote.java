/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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
