package com.example.replicated.multimap.domain;

import com.akkaserverless.javasdk.impl.JsonSerializer;
import com.akkaserverless.javasdk.impl.Serializers;
import com.akkaserverless.javasdk.replicatedentity.ReplicatedMultiMap;
import com.akkaserverless.javasdk.replicatedentity.ReplicatedMultiMapEntity;
import com.example.replicated.multimap.SomeMultiMapApi;
import com.google.protobuf.Empty;
import org.example.Components;
import org.example.ComponentsImpl;

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public abstract class AbstractSomeMultiMap extends ReplicatedMultiMapEntity<SomeKey, SomeValue> {

  protected final Components components() {
    return new ComponentsImpl(commandContext());
  }

  public abstract Effect<Empty> put(ReplicatedMultiMap<SomeKey, SomeValue> currentData, SomeMultiMapApi.PutValue putValue);

}
