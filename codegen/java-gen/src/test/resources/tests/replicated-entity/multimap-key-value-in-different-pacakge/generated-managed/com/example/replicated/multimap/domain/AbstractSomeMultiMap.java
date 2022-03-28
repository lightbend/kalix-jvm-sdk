package com.example.replicated.multimap.domain;

import com.example.replicated.multimap.SomeMultiMapApi;
import com.example.replicated.multimap.domain.key.SomeMultiMapDomainKey;
import com.example.replicated.multimap.domain.value.SomeMultiMapDomainValue;
import com.google.protobuf.Empty;
import kalix.javasdk.replicatedentity.ReplicatedMultiMap;
import kalix.javasdk.replicatedentity.ReplicatedMultiMapEntity;
import org.example.Components;
import org.example.ComponentsImpl;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public abstract class AbstractSomeMultiMap extends ReplicatedMultiMapEntity<SomeMultiMapDomainKey.SomeKey, SomeMultiMapDomainValue.SomeValue> {

  protected final Components components() {
    return new ComponentsImpl(commandContext());
  }

  public abstract Effect<Empty> put(ReplicatedMultiMap<SomeMultiMapDomainKey.SomeKey, SomeMultiMapDomainValue.SomeValue> currentData, SomeMultiMapApi.PutValue putValue);

}
