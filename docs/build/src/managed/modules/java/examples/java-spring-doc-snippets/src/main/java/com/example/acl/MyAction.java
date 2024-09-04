package com.example.acl;

import akka.Done;
import kalix.javasdk.action.Action;
import kalix.javasdk.valueentity.ValueEntity;
import kalix.javasdk.annotations.Acl;
import kalix.javasdk.annotations.Subscribe;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
public class MyAction extends Action {

    // tag::allow-deny[]
    @PostMapping
    @Acl(allow = @Acl.Matcher(service = "*"),
            deny = @Acl.Matcher(service = "service-b"))
    public Effect<String> createUser(@RequestBody CreateUser create) {
        //...
        // end::allow-deny[]
        return null;
        // tag::allow-deny[]
    }
    // end::allow-deny[]

    // tag::all-traffic[]
    @Acl(allow = @Acl.Matcher(principal = Acl.Principal.ALL))
    // end::all-traffic[]
    public void example2() {
    }

    // tag::internet[]
    @Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
    // end::internet[]
    public void example3() {
    }

    // tag::multiple-services[]
    @Acl(allow = {
            @Acl.Matcher(service = "service-a"),
            @Acl.Matcher(service = "service-b")})
    // end::multiple-services[]
    public void example4() {}

    // tag::block-traffic[]
    @Acl(allow = {})
    // end::block-traffic[]
    public void example5() {
    }

    // tag::deny-code[]
    @PostMapping
    @Acl(allow = @Acl.Matcher(service = "*"), denyCode = Acl.DenyStatusCode.NOT_FOUND)
    public Effect<String> updateUser(@RequestBody CreateUser create) {
        //...
        // end::deny-code[]
        return null;
        // tag::deny-code[]
    }
    // end::deny-code[]

    // tag::open-subscription-acl[]
    @Subscribe.ValueEntity(Counter.class)
    @PostMapping("/counter")
    @Acl(allow = @Acl.Matcher(service = "*")) 
    public Effect<Done> changes(@RequestBody CounterState counterState) {
     //...
        // end::open-subscription-acl[]
        return null;
        // tag::open-subscription-acl[]
    }
    // end::open-subscription-acl[]


}

class CreateUser{}
class Counter extends ValueEntity<Integer> {}
class CounterState{}
