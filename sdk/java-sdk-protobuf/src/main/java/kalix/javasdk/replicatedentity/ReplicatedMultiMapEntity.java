/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.replicatedentity;

public class ReplicatedMultiMapEntity<K, V> extends ReplicatedEntity<ReplicatedMultiMap<K, V>> {
  @Override
  public final ReplicatedMultiMap<K, V> emptyData(ReplicatedDataFactory factory) {
    return factory.newReplicatedMultiMap();
  }
}
