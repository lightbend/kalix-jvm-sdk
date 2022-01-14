package org.example

import com.akkaserverless.scalasdk.AkkaServerless
import com.akkaserverless.scalasdk.view.ViewCreationContext
import org.example.named.view.MyUserByNameView
import org.example.named.view.MyUserByNameViewProvider

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object AkkaServerlessFactory {

  def withComponents(
      createMyUserByNameView: ViewCreationContext => MyUserByNameView): AkkaServerless = {
    val akkaServerless = AkkaServerless()
    akkaServerless
      .register(MyUserByNameViewProvider(createMyUserByNameView))
  }
}
