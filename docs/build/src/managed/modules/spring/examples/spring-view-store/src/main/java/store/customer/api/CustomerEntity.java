package store.customer.api;

import store.customer.domain.Address;
import store.customer.domain.Customer;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.springsdk.annotations.EntityKey;
import kalix.springsdk.annotations.EntityType;
import kalix.springsdk.annotations.EventHandler;
import org.springframework.web.bind.annotation.*;

import static store.customer.domain.CustomerEvent.*;

@EntityType("customer")
@EntityKey("id")
@RequestMapping("/customer/{id}")
public class CustomerEntity extends EventSourcedEntity<Customer> {

  @GetMapping
  public Effect<Customer> get() {
    return effects().reply(currentState());
  }

  @PostMapping("/create")
  public Effect<String> create(@RequestBody Customer customer) {
    return effects()
        .emitEvent(new CustomerCreated(customer.email(), customer.name(), customer.address()))
        .thenReply(__ -> "OK");
  }

  @EventHandler
  public Customer onEvent(CustomerCreated created) {
    return new Customer(created.email(), created.name(), created.address());
  }

  @PostMapping("/changeName/{newName}")
  public Effect<String> changeName(@PathVariable String newName) {
    return effects().emitEvent(new CustomerNameChanged(newName)).thenReply(__ -> "OK");
  }

  @EventHandler
  public Customer onEvent(CustomerNameChanged customerNameChanged) {
    return currentState().withName(customerNameChanged.newName());
  }

  @PostMapping("/changeAddress")
  public Effect<String> changeAddress(@RequestBody Address newAddress) {
    return effects().emitEvent(new CustomerAddressChanged(newAddress)).thenReply(__ -> "OK");
  }

  @EventHandler
  public Customer onEvent(CustomerAddressChanged customerAddressChanged) {
    return currentState().withAddress(customerAddressChanged.newAddress());
  }
}
