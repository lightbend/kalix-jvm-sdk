package customer.views;

import kalix.javasdk.annotations.Acl;
import kalix.javasdk.annotations.Query;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.annotations.Table;
import kalix.javasdk.view.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Flux;

// tag::view[]
@Table("customers_by_email")
@Subscribe.Stream( // <1>
  service = "customer-registry", // <2>
  id = "customer_events", // <3>
  consumerGroup = "customer-by-email-view" // <4>
)
public class CustomersByEmailView extends View<Customer> {
  // end::view[]
  private static final Logger logger = LoggerFactory.getLogger(CustomersByEmailView.class);
  // tag::view[]

  public UpdateEffect<Customer> onEvent( // <4>
                                         CustomerPublicEvent.Created created) {
    // end::view[]
    logger.info("Received: {}", created);
    // tag::view[]
    var id = updateContext().eventSubject().get();
    return effects().updateState(
      new Customer(id, created.email(), created.name()));
  }

  public UpdateEffect<Customer> onEvent(
    CustomerPublicEvent.NameChanged nameChanged) {
    // end::view[]
    logger.info("Received: {}", nameChanged);
    // tag::view[]
    var updated = viewState().withName(nameChanged.newName());
    return effects().updateState(updated);
  }

  @GetMapping("/customers/by_email/{email}")
  @Query("SELECT * FROM customers_by_email WHERE email = :email")
  @Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
  public Flux<Customer> findByName(@PathVariable String email) {
    return null;
  }

}
// end::view[]
