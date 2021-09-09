package com.example.replicated.set.domain;

import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
import com.akkaserverless.javasdk.replicatedentity.ReplicatedSet;
import com.example.replicated.set.SomeSetApi;
import com.google.protobuf.Empty;

import java.util.Comparator;
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
  public Effect<Empty> add(
      ReplicatedSet<SomeSetDomain.SomeElement> set, SomeSetApi.AddElement command) {
    SomeSetDomain.SomeElement element = // <1>
        SomeSetDomain.SomeElement.newBuilder().setValue(command.getElement().getValue()).build();
    set.add(element); // <2>
    return effects()
        .update(set) // <3>
        .thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<Empty> remove(
      ReplicatedSet<SomeSetDomain.SomeElement> set, SomeSetApi.RemoveElement command) {
    SomeSetDomain.SomeElement element = // <1>
        SomeSetDomain.SomeElement.newBuilder().setValue(command.getElement().getValue()).build();
    set.remove(element); // <2>
    return effects()
        .update(set) // <3>
        .thenReply(Empty.getDefaultInstance());
  }
  // end::update[]

  // tag::get[]
  @Override
  public Effect<SomeSetApi.CurrentElements> get(
      ReplicatedSet<SomeSetDomain.SomeElement> set, SomeSetApi.GetElements command) {
    List<SomeSetApi.Element> elements =
        set.stream() // <1>
            .map(element -> SomeSetApi.Element.newBuilder().setValue(element.getValue()).build())
            .sorted(Comparator.comparing(SomeSetApi.Element::getValue))
            .collect(Collectors.toList());

    SomeSetApi.CurrentElements currentElements =
        SomeSetApi.CurrentElements.newBuilder().addAllElements(elements).build();

    return effects().reply(currentElements);
  }
  // end::get[]
}
