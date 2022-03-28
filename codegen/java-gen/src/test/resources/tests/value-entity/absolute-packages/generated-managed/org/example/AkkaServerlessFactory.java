package org.example;

import kalix.javasdk.AkkaServerless;
import kalix.javasdk.valueentity.ValueEntityContext;
import org.example.domain.Counter;
import org.example.domain.CounterProvider;
import org.example.valueentity.CounterApi;

import java.util.function.Function;

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public final class AkkaServerlessFactory {

  public static AkkaServerless withComponents(
      Function<ValueEntityContext, Counter> createCounter) {
    AkkaServerless akkaServerless = new AkkaServerless();
    return akkaServerless
      .register(CounterProvider.of(createCounter));
  }
}
