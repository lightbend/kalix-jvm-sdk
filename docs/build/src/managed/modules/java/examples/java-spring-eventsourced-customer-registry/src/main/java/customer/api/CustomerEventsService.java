package customer.api;

import customer.domain.CustomerEvent;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Publish;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.annotations.Acl;

// tag::producer[]
@Subscribe.EventSourcedEntity(
    value = CustomerEntity.class, // <1>
    ignoreUnknown = true) // <2>
@Publish.Stream(id = "customer_events") // <3>
@Acl(allow = @Acl.Matcher(service = "*")) // <4>
public class CustomerEventsService extends Action {

  public Effect<CustomerPublicEvent.Created> onEvent( // <5>
      CustomerEvent.CustomerCreated created) {
    return effects().reply(
        new CustomerPublicEvent.Created(created.email(), created.name()));
  }

  public Effect<CustomerPublicEvent.NameChanged> onEvent( // <5>
      CustomerEvent.NameChanged nameChanged) {
    return effects().reply(new CustomerPublicEvent.NameChanged(nameChanged.newName()));
  }
}
// end::producer[]