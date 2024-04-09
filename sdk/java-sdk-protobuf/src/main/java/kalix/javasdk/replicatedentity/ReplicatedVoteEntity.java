/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.replicatedentity;

public class ReplicatedVoteEntity extends ReplicatedEntity<ReplicatedVote> {
  @Override
  public final ReplicatedVote emptyData(ReplicatedDataFactory factory) {
    return factory.newVote();
  }
}
