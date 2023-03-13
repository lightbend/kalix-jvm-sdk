package customer.view;

// tag::class[]
import customer.domain.Customer;
import customer.api.CustomerEntity;
import kalix.javasdk.view.View;
import kalix.javasdk.annotations.Query;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.annotations.Table;
import kalix.javasdk.annotations.ViewId;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Flux;

@ViewId("view_customers_by_name") // <1>
@Table("customers_by_name")  // <2>
@Subscribe.ValueEntity(CustomerEntity.class) // <3>
public class CustomersByNameView extends View<Customer> { // <4>

  @GetMapping("/customer/by_name/{customer_name}")   // <5>
  @Query("SELECT * FROM customers_by_name WHERE name = :customer_name") // <6>
  public Flux<Customer> getCustomer(String name) { // <7>
    return null; // <8>
  }
}
// end::class[]
