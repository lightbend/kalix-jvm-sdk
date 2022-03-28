package org.example;

import kalix.javasdk.AkkaServerless;
import kalix.javasdk.action.ActionCreationContext;
import org.example.service.MyServiceNamedAction;
import org.example.service.MyServiceNamedActionProvider;
import org.example.service.ServiceOuterClass;

import java.util.function.Function;

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public final class AkkaServerlessFactory {

  public static AkkaServerless withComponents(
      Function<ActionCreationContext, MyServiceNamedAction> createMyServiceNamedAction) {
    AkkaServerless akkaServerless = new AkkaServerless();
    return akkaServerless
      .register(MyServiceNamedActionProvider.of(createMyServiceNamedAction));
  }
}
