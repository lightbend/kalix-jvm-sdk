package com.example.acl;

import kalix.javasdk.action.Action;
import kalix.springsdk.annotations.Acl;
// tag::acl[]
@Acl(denyCode = Acl.DenyStatusCode.NOT_FOUND_404)
public class UserAction extends Action {
    //...
}
// end::acl[]
