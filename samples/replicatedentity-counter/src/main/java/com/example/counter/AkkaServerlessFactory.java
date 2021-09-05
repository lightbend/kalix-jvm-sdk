// FIXME codegen for ReplicatedEntity

package com.example.counter;

import com.akkaserverless.javasdk.AkkaServerless;
import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
import com.example.counter.domain.Counter;
import com.example.counter.domain.CounterProvider;
import java.util.function.Function;

public final class AkkaServerlessFactory {

  public static AkkaServerless withComponents(
      Function<ReplicatedEntityContext, Counter> createCounter) {
    AkkaServerless akkaServerless = new AkkaServerless();
    return akkaServerless
      .register(CounterProvider.of(createCounter));
  }
}
