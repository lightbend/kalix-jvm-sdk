package org.example;

import kalix.javasdk.Kalix;
import kalix.javasdk.action.ActionCreationContext;
import org.example.service.MyServiceNamedAction;
import org.example.service.MyServiceNamedActionProvider;
import org.example.service.ServiceOuterClass;

import java.util.function.Function;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public final class KalixFactory {

  public static Kalix withComponents(
      Function<ActionCreationContext, MyServiceNamedAction> createMyServiceNamedAction) {
    Kalix kalix = new Kalix();
    return kalix
      .register(MyServiceNamedActionProvider.of(createMyServiceNamedAction));
  }
}
