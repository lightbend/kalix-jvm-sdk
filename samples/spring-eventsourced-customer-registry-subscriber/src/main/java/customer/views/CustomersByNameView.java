package customer.views;

import kalix.javasdk.view.View;
import kalix.javasdk.annotations.Acl;
import kalix.javasdk.annotations.Query;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.annotations.Table;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Flux;

// tag::view[]
@Table("customers_by_name")
@Subscribe.Stream( // <1>
    // end::view[]
    // Normally the Kalix service deployment name
    // (but localhost + hardcoded port here to be able to run both locally)
    service = "host.docker.internal:9000",
    /*
    // tag::view[]
    service = "customer-registry", // <2>
    // end::view[]
     */
    // tag::view[]
    id = "customer_events" // <3>
)
public class CustomersByNameView extends View<Customer> {

  public UpdateEffect<Customer> onEvent( // <4>
      CustomerPublicEvent.Created created) {
    var id = updateContext().eventSubject().get();
    return effects().updateState(
        new Customer(id, created.email(), created.name()));
  }

  public UpdateEffect<Customer> onEvent(
      CustomerPublicEvent.NameChanged nameChanged) {
    var updated = viewState().withName(nameChanged.newName());
    return effects().updateState(updated);
  }

  @GetMapping("/customers/by_name/{name}")
  @Query("SELECT * FROM customers_by_name WHERE name = :name")
  @Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
  public Flux<Customer> findByName(@PathVariable String name) {
    return null;
  }

}
// end::view[]
