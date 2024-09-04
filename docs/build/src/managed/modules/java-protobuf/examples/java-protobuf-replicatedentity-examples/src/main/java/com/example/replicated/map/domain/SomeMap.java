package com.example.replicated.map.domain;

import kalix.javasdk.replicatedentity.ReplicatedCounter;
import kalix.replicatedentity.ReplicatedData;
import kalix.javasdk.replicatedentity.ReplicatedEntityContext;
import kalix.javasdk.replicatedentity.ReplicatedMap;
import kalix.javasdk.replicatedentity.ReplicatedRegister;
import kalix.javasdk.replicatedentity.ReplicatedSet;
import com.example.replicated.map.SomeMapApi;
import com.google.protobuf.Empty;

import java.util.stream.Collectors;

public class SomeMap extends AbstractSomeMap {
  @SuppressWarnings("unused")
  private final String entityId;

  private static final SomeMapDomain.SomeKey FOO_KEY =
      SomeMapDomain.SomeKey.newBuilder().setSomeField("foo").build();

  private static final SomeMapDomain.SomeKey BAR_KEY =
      SomeMapDomain.SomeKey.newBuilder().setSomeField("bar").build();

  private static final SomeMapDomain.SomeKey BAZ_KEY =
      SomeMapDomain.SomeKey.newBuilder().setSomeField("baz").build();

  public SomeMap(ReplicatedEntityContext context) {
    this.entityId = context.entityId();
  }

  // tag::update[]
  @Override
  public Effect<Empty> increaseFoo(
      ReplicatedMap<SomeMapDomain.SomeKey, ReplicatedData> map,
      SomeMapApi.IncreaseFooValue command) {
    ReplicatedCounter foo = map.getReplicatedCounter(FOO_KEY); // <1>
    return effects()
        .update(map.update(FOO_KEY, foo.increment(command.getValue()))) // <2> <3>
        .thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<Empty> decreaseFoo(
      ReplicatedMap<SomeMapDomain.SomeKey, ReplicatedData> map,
      SomeMapApi.DecreaseFooValue command) {
    ReplicatedCounter foo = map.getReplicatedCounter(FOO_KEY); // <1>
    return effects()
        .update(map.update(FOO_KEY, foo.decrement(command.getValue()))) // <2> <3>
        .thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<Empty> setBar(
      ReplicatedMap<SomeMapDomain.SomeKey, ReplicatedData> map, SomeMapApi.SetBarValue command) {
    ReplicatedRegister<String> bar = map.getReplicatedRegister(BAR_KEY); // <1>
    return effects()
        .update(map.update(BAR_KEY, bar.set(command.getValue()))) // <2> <3>
        .thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<Empty> addBaz(
      ReplicatedMap<SomeMapDomain.SomeKey, ReplicatedData> map, SomeMapApi.AddBazValue command) {
    ReplicatedSet<String> baz = map.getReplicatedSet(BAZ_KEY); // <1>
    return effects()
        .update(map.update(BAZ_KEY, baz.add(command.getValue()))) // <2> <3>
        .thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<Empty> removeBaz(
      ReplicatedMap<SomeMapDomain.SomeKey, ReplicatedData> map, SomeMapApi.RemoveBazValue command) {
    ReplicatedSet<String> baz = map.getReplicatedSet(BAZ_KEY); // <1>
    return effects()
        .update(map.update(BAZ_KEY, baz.remove(command.getValue()))) // <2> <3>
        .thenReply(Empty.getDefaultInstance());
  }
  // end::update[]

  // tag::get[]
  @Override
  public Effect<SomeMapApi.CurrentValues> get(
      ReplicatedMap<SomeMapDomain.SomeKey, ReplicatedData> map, SomeMapApi.GetValues command) {
    ReplicatedCounter foo = map.getReplicatedCounter(FOO_KEY); // <1>
    ReplicatedRegister<String> bar = map.getReplicatedRegister(BAR_KEY, () -> ""); // <1>
    ReplicatedSet<String> baz = map.getReplicatedSet(BAZ_KEY); // <1>
    SomeMapApi.CurrentValues currentValues =
        SomeMapApi.CurrentValues.newBuilder()
            .setFoo(foo.getValue())
            .setBar(bar.get())
            .addAllBaz(baz.elements().stream().sorted().collect(Collectors.toList()))
            .build();
    return effects().reply(currentValues);
  }
  // end::get[]
}
