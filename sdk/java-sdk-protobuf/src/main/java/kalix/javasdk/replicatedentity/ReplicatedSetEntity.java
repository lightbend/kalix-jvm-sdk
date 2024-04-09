/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.replicatedentity;

public class ReplicatedSetEntity<T> extends ReplicatedEntity<ReplicatedSet<T>> {
  @Override
  public final ReplicatedSet<T> emptyData(ReplicatedDataFactory factory) {
    return factory.newReplicatedSet();
  }
}
