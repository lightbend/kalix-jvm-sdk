package org.example

import com.akkaserverless.scalasdk.AkkaServerless
import com.akkaserverless.scalasdk.view.ViewCreationContext
import org.example.view.UserByNameViewImpl
import org.example.view.UserByNameViewProvider

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object AkkaServerlessFactory {

  def withComponents(
      createUserByNameViewImpl: ViewCreationContext => UserByNameViewImpl): AkkaServerless = {
    val akkaServerless = AkkaServerless()
    akkaServerless
      .register(UserByNameViewProvider(createUserByNameViewImpl))
  }
}
