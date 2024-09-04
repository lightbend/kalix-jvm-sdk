package com.example;

import kalix.javasdk.action.ActionCreationContext;

// This class was initially generated based on the .proto definition by Kalix tooling.
// This is the implementation for the Action Service described in your com/example/jwt_service.proto file.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class JwtServiceActionImpl extends AbstractJwtServiceAction {

  public JwtServiceActionImpl(ActionCreationContext creationContext) {}

  @Override
  public Effect<JwtService.MyResponse> jwtInToken(JwtService.MyRequest myRequest) {
    if (actionContext().metadata().jwtClaims().subject().isEmpty())
      return effects().error("No subject");

    return effects().reply(
        JwtService.MyResponse.newBuilder().setMsg(myRequest.getMsg()).build());
  }

  @Override
  public Effect<JwtService.MyResponse> jwtInMessage(JwtService.MyRequestWithToken myRequestWithToken) {
    if (myRequestWithToken.getMyToken().isEmpty())
      return effects().error("No subject");

    return effects().reply(
        JwtService.MyResponse.newBuilder().setMsg(myRequestWithToken.getMsg()).build());
  }
}
