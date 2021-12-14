package org.example;

import com.akkaserverless.javasdk.AkkaServerless;
import com.akkaserverless.javasdk.action.ActionCreationContext;
import com.example.service.MyServiceAction;
import com.example.service.MyServiceActionProvider;
import com.example.service.ServiceOuterClass;

import java.util.function.Function;

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public final class AkkaServerlessFactory {

  public static AkkaServerless withComponents(
      Function<ActionCreationContext, MyServiceAction> createMyServiceAction) {
    AkkaServerless akkaServerless = new AkkaServerless();
    return akkaServerless
      .register(MyServiceActionProvider.of(createMyServiceAction));
  }
}
