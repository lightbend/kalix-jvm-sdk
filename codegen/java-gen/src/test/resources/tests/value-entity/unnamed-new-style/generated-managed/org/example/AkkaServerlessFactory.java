package org.example;

import com.akkaserverless.javasdk.AkkaServerless;
import com.akkaserverless.javasdk.valueentity.ValueEntityContext;
import org.example.valueentity.CounterApi;
import org.example.valueentity.CounterServiceEntity;
import org.example.valueentity.CounterServiceEntityProvider;

import java.util.function.Function;

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public final class AkkaServerlessFactory {

  public static AkkaServerless withComponents(
      Function<ValueEntityContext, CounterServiceEntity> createCounterServiceEntity) {
    AkkaServerless akkaServerless = new AkkaServerless();
    return akkaServerless
      .register(CounterServiceEntityProvider.of(createCounterServiceEntity));
  }
}
