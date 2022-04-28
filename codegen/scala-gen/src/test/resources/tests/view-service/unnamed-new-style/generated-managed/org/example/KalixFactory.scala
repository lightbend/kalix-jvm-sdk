package org.example

import kalix.scalasdk.Kalix
import kalix.scalasdk.view.ViewCreationContext
import org.example.unnamed.view.UserByNameView
import org.example.unnamed.view.UserByNameViewProvider

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object KalixFactory {

  def withComponents(
      createUserByNameView: ViewCreationContext => UserByNameView): Kalix = {
    val kalix = Kalix()
    kalix
      .register(UserByNameViewProvider(createUserByNameView))
  }
}
