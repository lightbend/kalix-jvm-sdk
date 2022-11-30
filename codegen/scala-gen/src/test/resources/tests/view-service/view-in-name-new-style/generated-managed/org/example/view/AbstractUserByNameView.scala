package org.example.view

import kalix.scalasdk.view.View

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

abstract class AbstractUserByNameView extends View[UserState] {
  def updateCustomer(
      state: UserState,
      userState: UserState): View.UpdateEffect[UserState]
}
