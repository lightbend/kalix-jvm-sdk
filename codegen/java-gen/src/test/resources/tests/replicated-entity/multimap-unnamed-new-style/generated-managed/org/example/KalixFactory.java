package org.example;

import com.example.replicated.multimap.MultiMapServiceEntity;
import com.example.replicated.multimap.MultiMapServiceEntityProvider;
import com.example.replicated.multimap.SomeMultiMapApi;
import kalix.javasdk.Kalix;
import kalix.javasdk.replicatedentity.ReplicatedEntityContext;

import java.util.function.Function;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public final class KalixFactory {

  public static Kalix withComponents(
      Function<ReplicatedEntityContext, MultiMapServiceEntity> createMultiMapServiceEntity) {
    Kalix kalix = new Kalix();
    return kalix
      .register(MultiMapServiceEntityProvider.of(createMultiMapServiceEntity));
  }
}
