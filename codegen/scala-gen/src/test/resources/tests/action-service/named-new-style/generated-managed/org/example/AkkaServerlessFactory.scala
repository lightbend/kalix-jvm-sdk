package org.example

import com.akkaserverless.scalasdk.AkkaServerless
import com.akkaserverless.scalasdk.action.ActionCreationContext
import org.example.service.example_action.MyServiceNamedAction
import org.example.service.example_action.MyServiceNamedActionProvider

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object AkkaServerlessFactory {

  def withComponents(
      createMyServiceNamedAction: ActionCreationContext => MyServiceNamedAction): AkkaServerless = {
    val akkaServerless = AkkaServerless()
    akkaServerless
      .register(MyServiceNamedActionProvider(createMyServiceNamedAction))
  }
}
