package com.example;

import kalix.javasdk.action.Action;
import kalix.springsdk.annotations.Acl;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
public class MyAction extends Action {

    // tag::acl1[]
    @PostMapping
    @Acl(allow = @Acl.Matcher(service = "*"),
            deny = @Acl.Matcher(service = "my-service"))
    public Effect<String> createUser(@RequestBody CreateUser create) {
        //...
        // end::acl1[]
        return null;
        // tag::acl1[]
    }
    // end::acl1[]

    // tag::acl2[]
    @Acl(allow = @Acl.Matcher(principal = Acl.Principal.ALL))
    // end::acl2[]
    public void example2() {
    }

    // tag::acl3[]
    @Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
    // end::acl3[]
    public void example3() {
    }

    // tag::acl4[]
    @Acl(allow = {
            @Acl.Matcher(service = "service-a"),
            @Acl.Matcher(service = "my-service")})
    // end::acl4[]
    public void example4() {}

    // tag::acl5[]
    @Acl(allow = {})
    // end::acl5[]
    public void example5() {
    }

    // tag::acl6[]
    @PostMapping
    @Acl(allow = @Acl.Matcher(service = "*"), denyCode = 5)
    public Effect<String> updateUser(@RequestBody CreateUser create) {
        //...
        // end::acl6[]
        return null;
        // tag::acl6[]
    }
    // end::acl6[]

// tag::acl[]
}
// end::acl[]
class CreateUser{}
