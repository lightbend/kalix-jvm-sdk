package com.example.replicated.multimap;

import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
import com.akkaserverless.javasdk.replicatedentity.ReplicatedMultiMap;
import com.google.protobuf.Empty;

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

/** A replicated entity. */
public class SomeMultiMap extends AbstractSomeMultiMap {
  @SuppressWarnings("unused")
  private final String entityId;

  public SomeMultiMap(ReplicatedEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public Effect<Empty> put(ReplicatedMultiMap<SomeMultiMapDomain.SomeKey, SomeMultiMapDomain.SomeValue> currentData, SomeMultiMapApi.PutValue putValue) {
    return effects().error("The command handler for `Put` is not implemented, yet");
  }
}
