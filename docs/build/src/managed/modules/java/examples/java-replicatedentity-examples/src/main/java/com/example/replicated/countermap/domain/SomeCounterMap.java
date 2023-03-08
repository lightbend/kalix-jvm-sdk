package com.example.replicated.countermap.domain;

import kalix.javasdk.replicatedentity.ReplicatedCounterMap;
import kalix.javasdk.replicatedentity.ReplicatedEntityContext;
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
      ReplicatedCounterMap<String> counterMap, SomeCounterMapApi.IncreaseValue command) {
    return effects()
        .update(counterMap.increment(command.getKey(), command.getValue())) // <1>
        .thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<Empty> decrease(
      ReplicatedCounterMap<String> counterMap, SomeCounterMapApi.DecreaseValue command) {
    return effects()
        .update(counterMap.decrement(command.getKey(), command.getValue())) // <1>
        .thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<Empty> remove(
      ReplicatedCounterMap<String> counterMap, SomeCounterMapApi.RemoveValue command) {
    return effects()
        .update(counterMap.remove(command.getKey())) // <1>
        .thenReply(Empty.getDefaultInstance());
  }
  // end::update[]

  // tag::get[]
  @Override
  public Effect<SomeCounterMapApi.CurrentValue> get(
      ReplicatedCounterMap<String> counterMap, SomeCounterMapApi.GetValue command) {
    long value = counterMap.get(command.getKey()); // <1>
    SomeCounterMapApi.CurrentValue currentValue =
        SomeCounterMapApi.CurrentValue.newBuilder().setValue(value).build();
    return effects().reply(currentValue);
  }

  @Override
  public Effect<SomeCounterMapApi.CurrentValues> getAll(
      ReplicatedCounterMap<String> counterMap, SomeCounterMapApi.GetAllValues command) {
    Map<String, Long> values =
        counterMap.keySet().stream() // <2>
            .map(key -> new SimpleEntry<>(key, counterMap.get(key)))
            .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    SomeCounterMapApi.CurrentValues currentValues =
        SomeCounterMapApi.CurrentValues.newBuilder().putAllValues(values).build();
    return effects().reply(currentValues);
  }
  // end::get[]
}
