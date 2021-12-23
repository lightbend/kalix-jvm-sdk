package org.example;

import com.akkaserverless.javasdk.AkkaServerless;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityContext;
import org.example.eventsourcedentity.CounterApi;
import org.example.eventsourcedentity.CounterServiceEntity;
import org.example.eventsourcedentity.CounterServiceEntityProvider;

import java.util.function.Function;

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public final class AkkaServerlessFactory {

  public static AkkaServerless withComponents(
      Function<EventSourcedEntityContext, CounterServiceEntity> createCounterServiceEntity) {
    AkkaServerless akkaServerless = new AkkaServerless();
    return akkaServerless
      .register(CounterServiceEntityProvider.of(createCounterServiceEntity));
  }
}
