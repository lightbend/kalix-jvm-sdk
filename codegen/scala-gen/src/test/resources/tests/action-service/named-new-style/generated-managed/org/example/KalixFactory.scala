package org.example

import kalix.scalasdk.Kalix
import kalix.scalasdk.action.ActionCreationContext
import org.example.service.MyServiceNamedAction
import org.example.service.MyServiceNamedActionProvider

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object KalixFactory {

  def withComponents(
      createMyServiceNamedAction: ActionCreationContext => MyServiceNamedAction): Kalix = {
    val kalix = Kalix()
    kalix
      .register(MyServiceNamedActionProvider(createMyServiceNamedAction))
  }
}
