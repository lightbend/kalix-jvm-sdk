package com.example.jwt;

import kalix.javasdk.action.Action;
import kalix.springsdk.annotations.JWT;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public class JWTAction extends Action {

        @PostMapping("/message")
        @JWT(validate = JWT.JwtMethodMode.BEARER_TOKEN,
                bearerTokenIssuer = {"a", "b"})
        public Action.Effect<String> message(@RequestBody String msg) {
            return effects().reply(msg);
        }
}
