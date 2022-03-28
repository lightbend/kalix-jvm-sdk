package org.example;

import com.example.replicated.multimap.MultiMapServiceEntity;
import com.example.replicated.multimap.MultiMapServiceEntityProvider;
import com.example.replicated.multimap.SomeMultiMapApi;
import kalix.javasdk.AkkaServerless;
import kalix.javasdk.replicatedentity.ReplicatedEntityContext;

import java.util.function.Function;

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public final class AkkaServerlessFactory {

  public static AkkaServerless withComponents(
      Function<ReplicatedEntityContext, MultiMapServiceEntity> createMultiMapServiceEntity) {
    AkkaServerless akkaServerless = new AkkaServerless();
    return akkaServerless
      .register(MultiMapServiceEntityProvider.of(createMultiMapServiceEntity));
  }
}
