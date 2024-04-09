/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.replicatedentity;

public class ReplicatedRegisterMapEntity<K, V>
    extends ReplicatedEntity<ReplicatedRegisterMap<K, V>> {
  @Override
  public final ReplicatedRegisterMap<K, V> emptyData(ReplicatedDataFactory factory) {
    return factory.newReplicatedRegisterMap();
  }
}
