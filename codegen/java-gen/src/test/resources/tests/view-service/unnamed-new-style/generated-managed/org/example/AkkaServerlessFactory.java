package org.example;

import kalix.javasdk.AkkaServerless;
import kalix.javasdk.view.ViewCreationContext;
import org.example.unnamed.view.UserByNameView;
import org.example.unnamed.view.UserByNameViewProvider;
import org.example.unnamed.view.UserViewModel;

import java.util.function.Function;

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public final class AkkaServerlessFactory {

  public static AkkaServerless withComponents(
      Function<ViewCreationContext, UserByNameView> createUserByNameView) {
    AkkaServerless akkaServerless = new AkkaServerless();
    return akkaServerless
      .register(UserByNameViewProvider.of(createUserByNameView));
  }
}
