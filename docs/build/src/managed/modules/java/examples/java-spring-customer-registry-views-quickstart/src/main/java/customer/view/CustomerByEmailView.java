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

@ViewId("view_customers_by_email") // <1>
@Table("customers_by_email") // <2>
@Subscribe.ValueEntity(CustomerEntity.class)// <3>
public class CustomerByEmailView extends View<Customer> { //  <4>

  @GetMapping("/customer/by_email/{email}") // <5>
  @Query("SELECT * FROM customers_by_email WHERE email = :email") // <6>
  public Customer getCustomer(String email) {
    return null; // <7>
  }
}
// end::class[]