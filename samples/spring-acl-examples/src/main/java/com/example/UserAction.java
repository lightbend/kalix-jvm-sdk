package com.example;

import kalix.javasdk.action.Action;
import kalix.springsdk.annotations.Acl;
// tag::acl[]
@Acl(denyCode = 5)
public class UserAction extends Action {
    //...
}
// end::acl[]
