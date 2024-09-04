package com.example.validated

import kalix.scalasdk.action.Action
import kalix.scalasdk.action.ActionCreationContext

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class ValidatedAction(creationContext: ActionCreationContext) extends AbstractValidatedAction {

  override def callMeMaybe(request: Request): Action.Effect[Response] = {
    effects.reply(Response(s"Hello ${request.email}, gosh what a valid email you sent me"))
  }
}
