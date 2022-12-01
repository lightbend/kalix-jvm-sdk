package org.example.view

import kalix.scalasdk.view.View.UpdateEffect
import kalix.scalasdk.view.ViewContext

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class UserByNameViewImpl(context: ViewContext) extends AbstractUserByNameView {

  override def emptyState: UserState =
    throw new UnsupportedOperationException("Not implemented yet, replace with your empty view state")

  override def updateCustomer(
      state: UserState,
      userState: UserState): UpdateEffect[UserState] =
    throw new UnsupportedOperationException("Update handler for 'UpdateCustomer' not implemented yet")

}
