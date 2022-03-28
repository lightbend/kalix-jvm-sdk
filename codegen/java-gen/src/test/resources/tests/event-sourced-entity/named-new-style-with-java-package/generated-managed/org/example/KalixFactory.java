package org.example;

import kalix.javasdk.Kalix;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import org.example.eventsourcedentity.CounterApi;
import org.example.eventsourcedentity.domain.Counter;
import org.example.eventsourcedentity.domain.CounterProvider;

import java.util.function.Function;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public final class KalixFactory {

  public static Kalix withComponents(
      Function<EventSourcedEntityContext, Counter> createCounter) {
    Kalix kalix = new Kalix();
    return kalix
      .register(CounterProvider.of(createCounter));
  }
}
