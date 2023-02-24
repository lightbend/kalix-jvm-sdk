package customer.view;

// tag::class[]
import customer.api.CustomerEntity;
import customer.domain.CustomerEvent;
import kalix.javasdk.view.View;
import kalix.javasdk.annotations.Query;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.annotations.Table;
import kalix.javasdk.annotations.ViewId;
import reactor.core.publisher.Flux;

import org.springframework.web.bind.annotation.GetMapping;

@ViewId("view_customers_by_name") // <1>
@Table("customers_by_name")
public class CustomerByNameView extends View<CustomerView> {

  @GetMapping("/customer/by_name/{customer_name}")
  @Query("SELECT * FROM customers_by_name WHERE name = :customer_name")
  public Flux<CustomerView> getCustomer(String name) {
    return null;
  }

  @Subscribe.EventSourcedEntity(CustomerEntity.class)
  public UpdateEffect<CustomerView> onEvent(CustomerEvent.CustomerCreated created) {
    return effects().updateState(new CustomerView(created.email(), created.name(), created.address()));
  }

  @Subscribe.EventSourcedEntity(CustomerEntity.class)
  public UpdateEffect<CustomerView> onEvent(CustomerEvent.NameChanged event) {
    return effects().updateState(viewState().withName(event.newName())); // <2>
  }

  @Subscribe.EventSourcedEntity(CustomerEntity.class)
  public UpdateEffect<CustomerView> onEvent(CustomerEvent.AddressChanged event) {
    return effects().updateState(viewState().withAddress(event.address()));
  }
}
// end::class[]
