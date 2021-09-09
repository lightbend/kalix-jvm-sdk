package com.example.replicated.multimap.domain;

import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
import com.akkaserverless.javasdk.replicatedentity.ReplicatedMultiMap;
import com.example.replicated.multimap.SomeMultiMapApi;
import com.google.protobuf.Empty;

import java.util.Comparator;
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
      ReplicatedMultiMap<SomeMultiMapDomain.SomeKey, SomeMultiMapDomain.SomeValue> multiMap,
      SomeMultiMapApi.PutValue command) {
    SomeMultiMapDomain.SomeKey key = // <1>
        SomeMultiMapDomain.SomeKey.newBuilder().setKey(command.getKey().getKey()).build();
    SomeMultiMapDomain.SomeValue value = // <2>
        SomeMultiMapDomain.SomeValue.newBuilder().setValue(command.getValue().getValue()).build();
    multiMap.put(key, value); // <3>
    return effects()
        .update(multiMap) // <4>
        .thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<Empty> putAll(
      ReplicatedMultiMap<SomeMultiMapDomain.SomeKey, SomeMultiMapDomain.SomeValue> multiMap,
      SomeMultiMapApi.PutAllValues command) {
    SomeMultiMapDomain.SomeKey key = // <1>
        SomeMultiMapDomain.SomeKey.newBuilder().setKey(command.getKey().getKey()).build();
    List<SomeMultiMapDomain.SomeValue> values = // <2>
        command.getValuesList().stream()
            .map(
                value ->
                    SomeMultiMapDomain.SomeValue.newBuilder().setValue(value.getValue()).build())
            .collect(Collectors.toList());
    multiMap.putAll(key, values); // <3>
    return effects()
        .update(multiMap) // <4>
        .thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<Empty> remove(
      ReplicatedMultiMap<SomeMultiMapDomain.SomeKey, SomeMultiMapDomain.SomeValue> multiMap,
      SomeMultiMapApi.RemoveValue command) {
    SomeMultiMapDomain.SomeKey key = // <1>
        SomeMultiMapDomain.SomeKey.newBuilder().setKey(command.getKey().getKey()).build();
    SomeMultiMapDomain.SomeValue value = // <2>
        SomeMultiMapDomain.SomeValue.newBuilder().setValue(command.getValue().getValue()).build();
    multiMap.remove(key, value); // <3>
    return effects()
        .update(multiMap) // <4>
        .thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<Empty> removeAll(
      ReplicatedMultiMap<SomeMultiMapDomain.SomeKey, SomeMultiMapDomain.SomeValue> multiMap,
      SomeMultiMapApi.RemoveAllValues command) {
    SomeMultiMapDomain.SomeKey key = // <1>
        SomeMultiMapDomain.SomeKey.newBuilder().setKey(command.getKey().getKey()).build();
    multiMap.removeAll(key); // <3>
    return effects()
        .update(multiMap) // <4>
        .thenReply(Empty.getDefaultInstance());
  }
  // end::update[]

  // tag::get[]
  @Override
  public Effect<SomeMultiMapApi.CurrentValues> get(
      ReplicatedMultiMap<SomeMultiMapDomain.SomeKey, SomeMultiMapDomain.SomeValue> multiMap,
      SomeMultiMapApi.GetValues command) {
    SomeMultiMapDomain.SomeKey key = // <1>
        SomeMultiMapDomain.SomeKey.newBuilder().setKey(command.getKey().getKey()).build();
    Set<SomeMultiMapDomain.SomeValue> values = multiMap.get(key); // <2>
    SomeMultiMapApi.CurrentValues currentValues =
        SomeMultiMapApi.CurrentValues.newBuilder()
            .addAllValues(
                values.stream()
                    .map(
                        value ->
                            SomeMultiMapApi.Value.newBuilder().setValue(value.getValue()).build())
                    .sorted(Comparator.comparing(SomeMultiMapApi.Value::getValue))
                    .collect(Collectors.toList()))
            .build();
    return effects().reply(currentValues);
  }

  @Override
  public Effect<SomeMultiMapApi.AllCurrentValues> getAll(
      ReplicatedMultiMap<SomeMultiMapDomain.SomeKey, SomeMultiMapDomain.SomeValue> multiMap,
      SomeMultiMapApi.GetAllValues command) {
    List<SomeMultiMapApi.CurrentValues> allValues =
        multiMap.keySet().stream() // <3>
            .map(
                key -> {
                  List<SomeMultiMapApi.Value> values =
                      multiMap.get(key).stream()
                          .map(
                              value ->
                                  SomeMultiMapApi.Value.newBuilder()
                                      .setValue(value.getValue())
                                      .build())
                          .sorted(Comparator.comparing(SomeMultiMapApi.Value::getValue))
                          .collect(Collectors.toList());
                  return SomeMultiMapApi.CurrentValues.newBuilder()
                      .setKey(SomeMultiMapApi.Key.newBuilder().setKey(key.getKey()))
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
