package org.example;

import kalix.javasdk.Kalix;
import kalix.javasdk.view.ViewCreationContext;
import org.example.view.UserByNameViewImpl;
import org.example.view.UserByNameViewProvider;
import org.example.view.UserViewModel;

import java.util.function.Function;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public final class KalixFactory {

  public static Kalix withComponents(
      Function<ViewCreationContext, UserByNameViewImpl> createUserByNameViewImpl) {
    Kalix kalix = new Kalix();
    return kalix
      .register(UserByNameViewProvider.of(createUserByNameViewImpl));
  }
}
