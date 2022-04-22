package org.example

import kalix.scalasdk.Kalix
import kalix.scalasdk.action.ActionCreationContext
import org.example.service.MyServiceAction
import org.example.service.MyServiceActionProvider

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object KalixFactory {

  def withComponents(
      createMyServiceAction: ActionCreationContext => MyServiceAction): Kalix = {
    val kalix = Kalix()
    kalix
      .register(MyServiceActionProvider(createMyServiceAction))
  }
}
