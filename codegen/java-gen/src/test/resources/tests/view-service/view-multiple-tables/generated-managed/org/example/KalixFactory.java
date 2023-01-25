package org.example;

import kalix.javasdk.Kalix;
import kalix.javasdk.view.ViewCreationContext;
import org.example.view.AnotherCustomerOrdersViewImpl;
import org.example.view.AnotherCustomerOrdersViewProvider;
import org.example.view.CustomerOrdersView;
import org.example.view.CustomerOrdersViewModel;
import org.example.view.CustomerOrdersViewProvider;

import java.util.function.Function;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public final class KalixFactory {

  public static Kalix withComponents(
      Function<ViewCreationContext, AnotherCustomerOrdersViewImpl> createAnotherCustomerOrdersViewImpl,
      Function<ViewCreationContext, CustomerOrdersView> createCustomerOrdersView) {
    Kalix kalix = new Kalix();
    return kalix
      .register(AnotherCustomerOrdersViewProvider.of(createAnotherCustomerOrdersViewImpl))
      .register(CustomerOrdersViewProvider.of(createCustomerOrdersView));
  }
}
