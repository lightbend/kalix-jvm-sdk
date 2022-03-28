package org.example

import kalix.scalasdk.Kalix
import kalix.scalasdk.view.ViewCreationContext
import org.example.view.UserByNameViewImpl
import org.example.view.UserByNameViewProvider

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object KalixFactory {

  def withComponents(
      createUserByNameViewImpl: ViewCreationContext => UserByNameViewImpl): Kalix = {
    val kalix = Kalix()
    kalix
      .register(UserByNameViewProvider(createUserByNameViewImpl))
  }
}
