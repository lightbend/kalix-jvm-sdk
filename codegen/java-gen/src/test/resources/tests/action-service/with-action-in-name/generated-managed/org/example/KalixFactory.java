package org.example;

import kalix.javasdk.Kalix;
import kalix.javasdk.action.ActionCreationContext;
import org.example.service.MyServiceActionImpl;
import org.example.service.MyServiceActionProvider;
import org.example.service.ServiceOuterClass;

import java.util.function.Function;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public final class KalixFactory {

  public static Kalix withComponents(
      Function<ActionCreationContext, MyServiceActionImpl> createMyServiceActionImpl) {
    Kalix kalix = new Kalix();
    return kalix
      .register(MyServiceActionProvider.of(createMyServiceActionImpl));
  }
}
