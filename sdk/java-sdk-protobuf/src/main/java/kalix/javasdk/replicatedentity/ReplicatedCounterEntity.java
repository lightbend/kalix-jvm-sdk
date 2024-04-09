/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.replicatedentity;

public class ReplicatedCounterEntity extends ReplicatedEntity<ReplicatedCounter> {
  @Override
  public final ReplicatedCounter emptyData(ReplicatedDataFactory factory) {
    return factory.newCounter();
  }
}
