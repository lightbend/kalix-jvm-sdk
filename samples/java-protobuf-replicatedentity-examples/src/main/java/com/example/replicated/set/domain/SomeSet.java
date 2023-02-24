package com.example.replicated.set.domain;

import kalix.javasdk.replicatedentity.ReplicatedEntityContext;
import kalix.javasdk.replicatedentity.ReplicatedSet;
import com.example.replicated.set.SomeSetApi;
import com.google.protobuf.Empty;

import java.util.List;
import java.util.stream.Collectors;

public class SomeSet extends AbstractSomeSet {
  @SuppressWarnings("unused")
  private final String entityId;

  public SomeSet(ReplicatedEntityContext context) {
    this.entityId = context.entityId();
  }

  // tag::update[]
  @Override
  public Effect<Empty> add(ReplicatedSet<String> set, SomeSetApi.AddElement command) {
    return effects()
        .update(set.add(command.getElement())) // <1>
        .thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<Empty> remove(ReplicatedSet<String> set, SomeSetApi.RemoveElement command) {
    return effects()
        .update(set.remove(command.getElement())) // <1>
        .thenReply(Empty.getDefaultInstance());
  }
  // end::update[]

  // tag::get[]
  @Override
  public Effect<SomeSetApi.CurrentElements> get(
      ReplicatedSet<String> set, SomeSetApi.GetElements command) {
    List<String> elements =
        set.elements().stream() // <1>
            .sorted()
            .collect(Collectors.toList());

    SomeSetApi.CurrentElements currentElements =
        SomeSetApi.CurrentElements.newBuilder().addAllElements(elements).build();

    return effects().reply(currentElements);
  }
  // end::get[]
}
