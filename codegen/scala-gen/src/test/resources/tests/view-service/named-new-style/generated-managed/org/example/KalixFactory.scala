package org.example

import kalix.scalasdk.Kalix
import kalix.scalasdk.view.ViewCreationContext
import org.example.named.view.MyUserByNameView
import org.example.named.view.MyUserByNameViewProvider

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object KalixFactory {

  def withComponents(
      createMyUserByNameView: ViewCreationContext => MyUserByNameView): Kalix = {
    val kalix = Kalix()
    kalix
      .register(MyUserByNameViewProvider(createMyUserByNameView))
  }
}
