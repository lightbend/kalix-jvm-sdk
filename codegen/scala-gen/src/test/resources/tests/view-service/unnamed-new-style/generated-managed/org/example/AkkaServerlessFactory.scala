package org.example

import com.akkaserverless.scalasdk.AkkaServerless
import com.akkaserverless.scalasdk.view.ViewCreationContext
import org.example.unnamed.view.UserByNameView
import org.example.unnamed.view.UserByNameViewProvider

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object AkkaServerlessFactory {

  def withComponents(
      createUserByNameView: ViewCreationContext => UserByNameView): AkkaServerless = {
    val akkaServerless = AkkaServerless()
    akkaServerless
      .register(UserByNameViewProvider(createUserByNameView))
  }
}
