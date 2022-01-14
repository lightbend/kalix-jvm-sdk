package org.example

import com.akkaserverless.scalasdk.AkkaServerless
import com.akkaserverless.scalasdk.action.ActionCreationContext
import org.example.service.MyServiceActionImpl
import org.example.service.MyServiceActionProvider

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object AkkaServerlessFactory {

  def withComponents(
      createMyServiceActionImpl: ActionCreationContext => MyServiceActionImpl): AkkaServerless = {
    val akkaServerless = AkkaServerless()
    akkaServerless
      .register(MyServiceActionProvider(createMyServiceActionImpl))
  }
}
