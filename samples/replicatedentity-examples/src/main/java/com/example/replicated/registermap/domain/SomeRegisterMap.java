package com.example.replicated.registermap.domain;

import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
import com.akkaserverless.javasdk.replicatedentity.ReplicatedRegisterMap;
import com.example.replicated.registermap.SomeRegisterMapApi;
import com.google.protobuf.Empty;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SomeRegisterMap extends AbstractSomeRegisterMap {
  @SuppressWarnings("unused")
  private final String entityId;

  public SomeRegisterMap(ReplicatedEntityContext context) {
    this.entityId = context.entityId();
  }

  // tag::update[]
  @Override
  public Effect<Empty> set(
      ReplicatedRegisterMap<SomeRegisterMapDomain.SomeKey, SomeRegisterMapDomain.SomeValue>
          registerMap,
      SomeRegisterMapApi.SetValue command) {
    SomeRegisterMapDomain.SomeKey key = // <1>
        SomeRegisterMapDomain.SomeKey.newBuilder().setKey(command.getKey().getKey()).build();
    SomeRegisterMapDomain.SomeValue value = // <2>
        SomeRegisterMapDomain.SomeValue.newBuilder()
            .setValue(command.getValue().getValue())
            .build();
    return effects()
        .update(registerMap.setValue(key, value)) // <3>
        .thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<Empty> remove(
      ReplicatedRegisterMap<SomeRegisterMapDomain.SomeKey, SomeRegisterMapDomain.SomeValue>
          registerMap,
      SomeRegisterMapApi.RemoveValue command) {
    SomeRegisterMapDomain.SomeKey key = // <1>
        SomeRegisterMapDomain.SomeKey.newBuilder().setKey(command.getKey().getKey()).build();
    return effects()
        .update(registerMap.remove(key)) // <3>
        .thenReply(Empty.getDefaultInstance());
  }
  // end::update[]

  // tag::get[]
  @Override
  public Effect<SomeRegisterMapApi.CurrentValue> get(
      ReplicatedRegisterMap<SomeRegisterMapDomain.SomeKey, SomeRegisterMapDomain.SomeValue>
          registerMap,
      SomeRegisterMapApi.GetValue command) {
    SomeRegisterMapDomain.SomeKey key = // <1>
        SomeRegisterMapDomain.SomeKey.newBuilder().setKey(command.getKey().getKey()).build();
    Optional<SomeRegisterMapDomain.SomeValue> maybeValue = registerMap.getValue(key); // <2>
    SomeRegisterMapApi.CurrentValue currentValue =
        SomeRegisterMapApi.CurrentValue.newBuilder()
            .setValue(
                SomeRegisterMapApi.Value.newBuilder()
                    .setValue(maybeValue.map(SomeRegisterMapDomain.SomeValue::getValue).orElse("")))
            .build();
    return effects().reply(currentValue);
  }

  @Override
  public Effect<SomeRegisterMapApi.CurrentValues> getAll(
      ReplicatedRegisterMap<SomeRegisterMapDomain.SomeKey, SomeRegisterMapDomain.SomeValue> registerMap,
      SomeRegisterMapApi.GetAllValues command) {
    List<SomeRegisterMapApi.CurrentValue> allValues =
        registerMap.keySet().stream() // <3>
            .map(
                key -> {
                  String value =
                      registerMap
                          .getValue(key)
                          .map(SomeRegisterMapDomain.SomeValue::getValue)
                          .orElse("");
                  return SomeRegisterMapApi.CurrentValue.newBuilder()
                      .setKey(SomeRegisterMapApi.Key.newBuilder().setKey(key.getKey()))
                      .setValue(SomeRegisterMapApi.Value.newBuilder().setValue(value))
                      .build();
                })
            .collect(Collectors.toList());
    SomeRegisterMapApi.CurrentValues currentValues =
        SomeRegisterMapApi.CurrentValues.newBuilder().addAllValues(allValues).build();
    return effects().reply(currentValues);
  }
  // end::get[]
}
