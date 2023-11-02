package com.example.jwt;

import kalix.javasdk.StatusCode;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.JWT;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public class JWTAction extends Action {

    // tag::bearer-token[]
    @PostMapping("/message")
    @JWT(validate = JWT.JwtMethodMode.BEARER_TOKEN) // <1>
    public Action.Effect<String> message(@RequestBody String msg) {
        return effects().reply(msg);
    }
    // end::bearer-token[]

    // tag::bearer-token-issuer[]
    @PostMapping("/message")
    @JWT(validate = JWT.JwtMethodMode.BEARER_TOKEN,
         bearerTokenIssuer = "my-issuer")       // <1>
    public Action.Effect<String> messageWithIssuer(@RequestBody String msg) {
        return effects().reply(msg);
    }
    // end::bearer-token-issuer[]


    // tag::bearer-token-multi-issuer[]
    @PostMapping("/message")
    @JWT(validate = JWT.JwtMethodMode.BEARER_TOKEN,
         bearerTokenIssuer = {"my-issuer", "my-other-issuer"}) // <1>
    public Action.Effect<String> messageWithMultiIssuer(@RequestBody String msg) {
        return effects().reply(msg);
    }
    // end::bearer-token-multi-issuer[]

    // tag::bearer-token-claims[]
    @PostMapping("/message")
    @JWT(validate = JWT.JwtMethodMode.BEARER_TOKEN)
    public Action.Effect<String> messageWithClaimValidation(@RequestBody String msg) {
        var maybeSubject = actionContext().metadata().jwtClaims().subject();
        if (maybeSubject.isEmpty())
            return effects().error("No subject present", StatusCode.ErrorCode.UNAUTHORIZED);

        return effects().reply(msg);
    }
    // end::bearer-token-claims[]
}
