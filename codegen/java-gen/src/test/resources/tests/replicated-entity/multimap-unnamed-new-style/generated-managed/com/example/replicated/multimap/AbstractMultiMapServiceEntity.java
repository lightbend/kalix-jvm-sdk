package com.example.replicated.multimap;

import com.example.replicated.multimap.domain.SomeMultiMapDomain;
import com.google.protobuf.Empty;
import kalix.javasdk.replicatedentity.ReplicatedMultiMap;
import kalix.javasdk.replicatedentity.ReplicatedMultiMapEntity;
import org.example.Components;
import org.example.ComponentsImpl;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public abstract class AbstractMultiMapServiceEntity extends ReplicatedMultiMapEntity<SomeMultiMapDomain.SomeKey, SomeMultiMapDomain.SomeValue> {

  protected final Components components() {
    return new ComponentsImpl(commandContext());
  }

  public abstract Effect<Empty> put(ReplicatedMultiMap<SomeMultiMapDomain.SomeKey, SomeMultiMapDomain.SomeValue> currentData, SomeMultiMapApi.PutValue putValue);

}
