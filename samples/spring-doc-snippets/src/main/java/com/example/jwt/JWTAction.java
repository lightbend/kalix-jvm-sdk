package com.example.jwt;

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
}
