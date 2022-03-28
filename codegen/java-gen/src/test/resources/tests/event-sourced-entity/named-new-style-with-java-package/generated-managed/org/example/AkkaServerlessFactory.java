package org.example;

import kalix.javasdk.AkkaServerless;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import org.example.eventsourcedentity.CounterApi;
import org.example.eventsourcedentity.domain.Counter;
import org.example.eventsourcedentity.domain.CounterProvider;

import java.util.function.Function;

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public final class AkkaServerlessFactory {

  public static AkkaServerless withComponents(
      Function<EventSourcedEntityContext, Counter> createCounter) {
    AkkaServerless akkaServerless = new AkkaServerless();
    return akkaServerless
      .register(CounterProvider.of(createCounter));
  }
}
