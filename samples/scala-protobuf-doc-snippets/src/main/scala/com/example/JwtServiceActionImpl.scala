package com.example

import kalix.scalasdk.action.Action
import kalix.scalasdk.action.ActionCreationContext

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class JwtServiceActionImpl(creationContext: ActionCreationContext) extends AbstractJwtServiceAction {

  override def jwtInToken(myRequest: MyRequest): Action.Effect[MyResponse] = {
    if (actionContext.metadata.jwtClaims.subject.isEmpty)
      effects.error("No subject")
    else {
      effects.reply(MyResponse(myRequest.msg))
    }
  }

  override def jwtInMessage(myRequestWithToken: MyRequestWithToken): Action.Effect[MyResponse] = {
    if (myRequestWithToken.subject.isEmpty)
      effects.error("No subject")
    else {
      effects.reply(MyResponse(myRequestWithToken.msg))
    }
  }
}
