package customer.api;

import customer.domain.Address;
import customer.domain.Customer;
import customer.domain.CustomerEvent;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.annotations.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import static customer.domain.CustomerEvent.*;

@Id("id")
@TypeId("customer")
@RequestMapping("/customer/{id}")
public class CustomerEntity extends EventSourcedEntity<Customer, CustomerEvent> {
  private static final Logger logger = LoggerFactory.getLogger(CustomerEntity.class);

  record Confirm(String msg){
    public static Confirm done = new Confirm("done");
  }

  @GetMapping
  public Effect<Customer> getCustomer() {
    return effects().reply(currentState());
  }

  @PostMapping("/create")
  public Effect<Confirm> create(@RequestBody Customer customer) {
    logger.info("Creating {}", customer);
    return effects()
        .emitEvent(new CustomerCreated(customer.email(), customer.name(), customer.address()))
        .thenReply(__ -> Confirm.done);
  }

  @EventHandler
  public Customer onEvent(CustomerCreated created) {
    return new Customer(created.email(), created.name(), created.address());
  }


  @PostMapping("/changeName/{newName}")
  public Effect<Confirm> changeName(@PathVariable String newName) {

    return effects()
        .emitEvent(new NameChanged(newName))
        .thenReply(__ -> Confirm.done);
  }

  @EventHandler
  public Customer onEvent(NameChanged nameChanged) {
    return currentState().withName(nameChanged.newName());
  }


  @PostMapping("/changeAddress")
  public Effect<Confirm> changeAddress(@RequestBody Address newAddress) {
    return effects()
        .emitEvent(new AddressChanged(newAddress))
        .thenReply(__ -> Confirm.done);
  }

  @EventHandler
  public Customer onEvents(AddressChanged addressChanged) {
    return currentState().withAddress(addressChanged.address());
  }
}
