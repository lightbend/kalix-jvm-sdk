package com.example.replicated.multimap;

import com.akkaserverless.javasdk.replicatedentity.ReplicatedMultiMap;
import com.akkaserverless.javasdk.replicatedentity.ReplicatedMultiMapEntity;
import com.google.protobuf.Empty;
import org.example.Components;
import org.example.ComponentsImpl;

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public abstract class AbstractSomeMultiMap extends ReplicatedMultiMapEntity<SomeMultiMapDomain.SomeKey, SomeMultiMapDomain.SomeValue> {

  protected final Components components() {
    return new ComponentsImpl(commandContext());
  }

  public abstract Effect<Empty> put(ReplicatedMultiMap<SomeMultiMapDomain.SomeKey, SomeMultiMapDomain.SomeValue> currentData, SomeMultiMapApi.PutValue putValue);

}
