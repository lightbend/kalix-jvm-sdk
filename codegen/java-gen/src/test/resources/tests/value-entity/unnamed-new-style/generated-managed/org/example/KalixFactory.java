package org.example;

import kalix.javasdk.Kalix;
import kalix.javasdk.valueentity.ValueEntityContext;
import org.example.valueentity.CounterApi;
import org.example.valueentity.CounterServiceEntity;
import org.example.valueentity.CounterServiceEntityProvider;

import java.util.function.Function;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public final class KalixFactory {

  public static Kalix withComponents(
      Function<ValueEntityContext, CounterServiceEntity> createCounterServiceEntity) {
    Kalix kalix = new Kalix();
    return kalix
      .register(CounterServiceEntityProvider.of(createCounterServiceEntity));
  }
}
