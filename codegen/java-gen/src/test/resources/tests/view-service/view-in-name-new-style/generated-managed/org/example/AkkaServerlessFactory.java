package org.example;

import kalix.javasdk.AkkaServerless;
import kalix.javasdk.view.ViewCreationContext;
import org.example.view.UserByNameViewImpl;
import org.example.view.UserByNameViewProvider;
import org.example.view.UserViewModel;

import java.util.function.Function;

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public final class AkkaServerlessFactory {

  public static AkkaServerless withComponents(
      Function<ViewCreationContext, UserByNameViewImpl> createUserByNameViewImpl) {
    AkkaServerless akkaServerless = new AkkaServerless();
    return akkaServerless
      .register(UserByNameViewProvider.of(createUserByNameViewImpl));
  }
}
