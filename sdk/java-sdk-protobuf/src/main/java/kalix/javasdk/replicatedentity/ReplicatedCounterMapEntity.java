/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.replicatedentity;

public class ReplicatedCounterMapEntity<K> extends ReplicatedEntity<ReplicatedCounterMap<K>> {
  @Override
  public final ReplicatedCounterMap<K> emptyData(ReplicatedDataFactory factory) {
    return factory.newReplicatedCounterMap();
  }
}
