package org.example;

import com.example.replicated.multimap.SomeMultiMap;
import com.example.replicated.multimap.SomeMultiMapApi;
import com.example.replicated.multimap.SomeMultiMapProvider;
import kalix.javasdk.Kalix;
import kalix.javasdk.replicatedentity.ReplicatedEntityContext;

import java.util.function.Function;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public final class KalixFactory {

  public static Kalix withComponents(
      Function<ReplicatedEntityContext, SomeMultiMap> createSomeMultiMap) {
    Kalix kalix = new Kalix();
    return kalix
      .register(SomeMultiMapProvider.of(createSomeMultiMap));
  }
}
