package org.example;

import kalix.javasdk.Kalix;
import kalix.javasdk.view.ViewCreationContext;
import org.example.named.view.MyUserByNameView;
import org.example.named.view.MyUserByNameViewProvider;
import org.example.named.view.UserViewModel;

import java.util.function.Function;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public final class KalixFactory {

  public static Kalix withComponents(
      Function<ViewCreationContext, MyUserByNameView> createMyUserByNameView) {
    Kalix kalix = new Kalix();
    return kalix
      .register(MyUserByNameViewProvider.of(createMyUserByNameView));
  }
}
