package org.example;

import kalix.javasdk.Kalix;
import kalix.javasdk.view.ViewCreationContext;
import org.example.unnamed.view.UserByNameView;
import org.example.unnamed.view.UserByNameViewProvider;
import org.example.unnamed.view.UserViewModel;

import java.util.function.Function;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public final class KalixFactory {

  public static Kalix withComponents(
      Function<ViewCreationContext, UserByNameView> createUserByNameView) {
    Kalix kalix = new Kalix();
    return kalix
      .register(UserByNameViewProvider.of(createUserByNameView));
  }
}
