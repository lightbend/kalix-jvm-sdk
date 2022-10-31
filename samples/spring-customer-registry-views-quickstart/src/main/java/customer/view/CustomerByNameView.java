package customer.view;

// tag::class[]
import customer.api.Customer;
import customer.api.CustomerEntity;
import kalix.javasdk.view.View;
import kalix.springsdk.annotations.Query;
import kalix.springsdk.annotations.Subscribe;
import kalix.springsdk.annotations.Table;
import org.springframework.web.bind.annotation.GetMapping;

@Table("customers_by_name")  // <1>
@Subscribe.ValueEntity(CustomerEntity.class) // <2>
public class CustomerByNameView extends View<Customer> { // <3>

  @GetMapping("/customer/by_name/{customer_name}")   // <4>
  @Query("SELECT * FROM customers_by_name WHERE name = :customer_name") // <5>
  public Customer getCustomer(String name) {
    return null; // <6>
  }
}
// end::class[]
