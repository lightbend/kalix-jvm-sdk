package org.example;

import com.akkaserverless.javasdk.AkkaServerless;
import com.akkaserverless.javasdk.action.ActionCreationContext;
import org.example.service.MyServiceActionImpl;
import org.example.service.MyServiceActionProvider;
import org.example.service.ServiceOuterClass;

import java.util.function.Function;

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public final class AkkaServerlessFactory {

  public static AkkaServerless withComponents(
      Function<ActionCreationContext, MyServiceActionImpl> createMyServiceActionImpl) {
    AkkaServerless akkaServerless = new AkkaServerless();
    return akkaServerless
      .register(MyServiceActionProvider.of(createMyServiceActionImpl));
  }
}
