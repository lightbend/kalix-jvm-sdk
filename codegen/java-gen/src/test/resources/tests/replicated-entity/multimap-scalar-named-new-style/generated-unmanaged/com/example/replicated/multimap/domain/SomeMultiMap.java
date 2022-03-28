package com.example.replicated.multimap.domain;

import kalix.javasdk.replicatedentity.ReplicatedEntityContext;
import kalix.javasdk.replicatedentity.ReplicatedMultiMap;
import com.example.replicated.multimap.SomeMultiMapApi;
import com.google.protobuf.Empty;

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
// This is the implementation for the Replicated Entity Service described in your multi_map_api.proto file.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class SomeMultiMap extends AbstractSomeMultiMap {
  @SuppressWarnings("unused")
  private final String entityId;

  public SomeMultiMap(ReplicatedEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public Effect<Empty> put(ReplicatedMultiMap<String, Double> currentData, SomeMultiMapApi.PutValue putValue) {
    return effects().error("The command handler for `Put` is not implemented, yet");
  }
}
