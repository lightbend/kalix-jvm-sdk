package com.example.replicated.multimap.domain;

import kalix.javasdk.replicatedentity.ReplicatedEntityContext;
import kalix.javasdk.replicatedentity.ReplicatedMultiMap;
import com.example.replicated.multimap.SomeMultiMapApi;
import com.google.protobuf.Empty;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SomeMultiMap extends AbstractSomeMultiMap {
  @SuppressWarnings("unused")
  private final String entityId;

  public SomeMultiMap(ReplicatedEntityContext context) {
    this.entityId = context.entityId();
  }

  // tag::update[]
  @Override
  public Effect<Empty> put(
      ReplicatedMultiMap<String, Double> multiMap, SomeMultiMapApi.PutValue command) {
    return effects()
        .update(multiMap.put(command.getKey(), command.getValue())) // <1>
        .thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<Empty> putAll(
      ReplicatedMultiMap<String, Double> multiMap, SomeMultiMapApi.PutAllValues command) {
    return effects()
        .update(multiMap.putAll(command.getKey(), command.getValuesList())) // <1>
        .thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<Empty> remove(
      ReplicatedMultiMap<String, Double> multiMap, SomeMultiMapApi.RemoveValue command) {
    return effects()
        .update(multiMap.remove(command.getKey(), command.getValue())) // <1>
        .thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<Empty> removeAll(
      ReplicatedMultiMap<String, Double> multiMap, SomeMultiMapApi.RemoveAllValues command) {
    return effects()
        .update(multiMap.removeAll(command.getKey())) // <1>
        .thenReply(Empty.getDefaultInstance());
  }
  // end::update[]

  // tag::get[]
  @Override
  public Effect<SomeMultiMapApi.CurrentValues> get(
      ReplicatedMultiMap<String, Double> multiMap, SomeMultiMapApi.GetValues command) {
    Set<Double> values = multiMap.get(command.getKey()); // <1>
    SomeMultiMapApi.CurrentValues currentValues =
        SomeMultiMapApi.CurrentValues.newBuilder()
            .addAllValues(values.stream().sorted().collect(Collectors.toList()))
            .build();
    return effects().reply(currentValues);
  }

  @Override
  public Effect<SomeMultiMapApi.AllCurrentValues> getAll(
      ReplicatedMultiMap<String, Double> multiMap, SomeMultiMapApi.GetAllValues command) {
    List<SomeMultiMapApi.CurrentValues> allValues =
        multiMap.keySet().stream() // <2>
            .map(
                key -> {
                  List<Double> values =
                      multiMap.get(key).stream().sorted().collect(Collectors.toList());
                  return SomeMultiMapApi.CurrentValues.newBuilder()
                      .setKey(key)
                      .addAllValues(values)
                      .build();
                })
            .collect(Collectors.toList());
    SomeMultiMapApi.AllCurrentValues allCurrentValues =
        SomeMultiMapApi.AllCurrentValues.newBuilder().addAllValues(allValues).build();
    return effects().reply(allCurrentValues);
  }
  // end::get[]
}
