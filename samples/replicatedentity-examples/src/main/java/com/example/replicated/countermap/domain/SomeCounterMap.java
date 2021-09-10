package com.example.replicated.countermap.domain;

import com.akkaserverless.javasdk.replicatedentity.ReplicatedCounterMap;
import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
import com.example.replicated.countermap.SomeCounterMapApi;
import com.google.protobuf.Empty;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.stream.Collectors;

public class SomeCounterMap extends AbstractSomeCounterMap {
  @SuppressWarnings("unused")
  private final String entityId;

  public SomeCounterMap(ReplicatedEntityContext context) {
    this.entityId = context.entityId();
  }

  // tag::update[]
  @Override
  public Effect<Empty> increase(
      ReplicatedCounterMap<SomeCounterMapDomain.SomeKey> counterMap,
      SomeCounterMapApi.IncreaseValue command) {
    SomeCounterMapDomain.SomeKey key = // <1>
        SomeCounterMapDomain.SomeKey.newBuilder().setKey(command.getKey()).build();
    return effects()
        .update(counterMap.increment(key, command.getValue())) // <2>
        .thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<Empty> decrease(
      ReplicatedCounterMap<SomeCounterMapDomain.SomeKey> counterMap,
      SomeCounterMapApi.DecreaseValue command) {
    SomeCounterMapDomain.SomeKey key = // <1>
        SomeCounterMapDomain.SomeKey.newBuilder().setKey(command.getKey()).build();
    return effects()
        .update(counterMap.decrement(key, command.getValue())) // <2>
        .thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<Empty> remove(
      ReplicatedCounterMap<SomeCounterMapDomain.SomeKey> counterMap,
      SomeCounterMapApi.RemoveValue command) {
    SomeCounterMapDomain.SomeKey key = // <1>
        SomeCounterMapDomain.SomeKey.newBuilder().setKey(command.getKey()).build();
    return effects()
        .update(counterMap.remove(key)) // <2>
        .thenReply(Empty.getDefaultInstance());
  }
  // end::update[]

  // tag::get[]
  @Override
  public Effect<SomeCounterMapApi.CurrentValue> get(
      ReplicatedCounterMap<SomeCounterMapDomain.SomeKey> counterMap,
      SomeCounterMapApi.GetValue command) {
    SomeCounterMapDomain.SomeKey key = // <1>
        SomeCounterMapDomain.SomeKey.newBuilder().setKey(command.getKey()).build();
    long value = counterMap.get(key); // <2>
    SomeCounterMapApi.CurrentValue currentValue =
        SomeCounterMapApi.CurrentValue.newBuilder().setValue(value).build();
    return effects().reply(currentValue);
  }

  @Override
  public Effect<SomeCounterMapApi.CurrentValues> getAll(
      ReplicatedCounterMap<SomeCounterMapDomain.SomeKey> counterMap,
      SomeCounterMapApi.GetAllValues command) {
    Map<String, Long> values =
        counterMap.keySet().stream() // <3>
            .map(key -> new SimpleEntry<>(key.getKey(), counterMap.get(key)))
            .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    SomeCounterMapApi.CurrentValues currentValues =
        SomeCounterMapApi.CurrentValues.newBuilder().putAllValues(values).build();
    return effects().reply(currentValues);
  }
  // end::get[]
}
