package org.example;

import com.akkaserverless.javasdk.AkkaServerless;
import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
import com.example.replicated.multimap.SomeMultiMap;
import com.example.replicated.multimap.SomeMultiMapApi;
import com.example.replicated.multimap.SomeMultiMapProvider;

import java.util.function.Function;

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public final class AkkaServerlessFactory {

  public static AkkaServerless withComponents(
      Function<ReplicatedEntityContext, SomeMultiMap> createSomeMultiMap) {
    AkkaServerless akkaServerless = new AkkaServerless();
    return akkaServerless
      .register(SomeMultiMapProvider.of(createSomeMultiMap));
  }
}
