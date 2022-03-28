package com.example.replicated.multimap;

import kalix.javasdk.replicatedentity.ReplicatedEntityContext;
import kalix.javasdk.replicatedentity.ReplicatedMultiMap;
import com.example.replicated.multimap.domain.SomeMultiMapDomain;
import com.google.protobuf.Empty;

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
// This is the implementation for the Replicated Entity Service described in your multi_map_api.proto file.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class MultiMapServiceEntity extends AbstractMultiMapServiceEntity {
  @SuppressWarnings("unused")
  private final String entityId;

  public MultiMapServiceEntity(ReplicatedEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public Effect<Empty> put(ReplicatedMultiMap<SomeMultiMapDomain.SomeKey, SomeMultiMapDomain.SomeValue> currentData, SomeMultiMapApi.PutValue putValue) {
    return effects().error("The command handler for `Put` is not implemented, yet");
  }
}
