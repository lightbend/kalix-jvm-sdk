package com.example.acl;

import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Acl;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// tag::acl1[]
// tag::acl[]
@Acl(allow = @Acl.Matcher(service = "service-a"))
public class EmployeeAction extends Action {
    //...
// end::acl[]

    @PostMapping
    @Acl(allow = @Acl.Matcher(service = "service-b"))
    public Effect<String> createEmployee(@RequestBody CreateEmployee create) {
        //...
// end::acl1[]
        return null;
// tag::acl1[]
    }
// tag::acl[]
}
// end::acl[]
// end::acl1[]
class CreateEmployee{}