package customer.api;

import customer.domain.Address;
import customer.domain.Customer;
import customer.domain.CustomerEvent;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.annotations.EntityKey;
import kalix.javasdk.annotations.EntityType;
import kalix.javasdk.annotations.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import static customer.domain.CustomerEvent.*;

@EntityKey("id")
@EntityType("customer")
@RequestMapping("/customer/{id}")
public class CustomerEntity extends EventSourcedEntity<Customer, CustomerEvent> {
  private static final Logger logger = LoggerFactory.getLogger(CustomerEntity.class);

  @GetMapping
  public Effect<Customer> getCustomer() {
    return effects().reply(currentState());
  }

  @PostMapping("/create")
  public Effect<String> create(@RequestBody Customer customer) {
    logger.info("Creating {}", customer);
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

    return effects()
        .emitEvent(new NameChanged(newName))
        .thenReply(__ -> "OK");
  }

  @EventHandler
  public Customer onEvent(NameChanged nameChanged) {
    return currentState().withName(nameChanged.newName());
  }


  @PostMapping("/changeAddress")
  public Effect<String> changeAddress(@RequestBody Address newAddress) {
    return effects()
        .emitEvent(new AddressChanged(newAddress))
        .thenReply(__ -> "OK");
  }

  @EventHandler
  public Customer onEvents(AddressChanged addressChanged) {
    return currentState().withAddress(addressChanged.address());
  }
}
