package com.example.acl;

import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Acl;
// tag::acl[]
@Acl(denyCode = Acl.DenyStatusCode.NOT_FOUND)
public class UserAction extends Action {
    //...
}
// end::acl[]
