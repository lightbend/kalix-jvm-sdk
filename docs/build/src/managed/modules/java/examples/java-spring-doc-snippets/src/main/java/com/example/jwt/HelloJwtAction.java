package com.example.jwt;

import kalix.javasdk.StatusCode;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.JWT;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// tag::bearer-token[]
@JWT(validate = JWT.JwtMethodMode.BEARER_TOKEN, 
     bearerTokenIssuer = "my-issuer") // <1>
public class HelloJwtAction extends Action {
    
    @PostMapping("/message") // <2>
    public Action.Effect<String> message(@RequestBody String msg) { 
        //..
    // end::bearer-token[]    
        return effects().reply(msg);
    // tag::bearer-token[]
    }
    
    @PostMapping("/message/issuer")
    @JWT(validate = JWT.JwtMethodMode.BEARER_TOKEN,
         bearerTokenIssuer = "my-other-issuer")     
    public Action.Effect<String> messageWithIssuer(@RequestBody String msg) { // <3>
        //..
    // end::bearer-token[]    
        return effects().reply(msg);
    // tag::bearer-token[]        
    }  
}
// end::bearer-token[]
