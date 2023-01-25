package org.example

import kalix.scalasdk.Kalix
import kalix.scalasdk.view.ViewCreationContext
import org.example.view.AnotherCustomerOrdersViewImpl
import org.example.view.AnotherCustomerOrdersViewProvider
import org.example.view.CustomerOrdersView
import org.example.view.CustomerOrdersViewProvider

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object KalixFactory {

  def withComponents(
      createAnotherCustomerOrdersViewImpl: ViewCreationContext => AnotherCustomerOrdersViewImpl,
      createCustomerOrdersView: ViewCreationContext => CustomerOrdersView): Kalix = {
    val kalix = Kalix()
    kalix
      .register(AnotherCustomerOrdersViewProvider(createAnotherCustomerOrdersViewImpl))
      .register(CustomerOrdersViewProvider(createCustomerOrdersView))
  }
}
