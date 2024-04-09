/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.replicatedentity;

import kalix.replicatedentity.ReplicatedData;

public class ReplicatedMapEntity<K, V extends ReplicatedData>
    extends ReplicatedEntity<ReplicatedMap<K, V>> {
  @Override
  public final ReplicatedMap<K, V> emptyData(ReplicatedDataFactory factory) {
    return factory.newReplicatedMap();
  }
}
