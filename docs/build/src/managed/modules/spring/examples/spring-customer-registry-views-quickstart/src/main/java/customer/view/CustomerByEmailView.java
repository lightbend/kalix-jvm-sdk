package customer.view;

import customer.api.Customer;
import customer.api.CustomerEntity;
import kalix.javasdk.view.View;
import kalix.springsdk.annotations.Query;
import kalix.springsdk.annotations.Subscribe;
import kalix.springsdk.annotations.Table;
import kalix.springsdk.annotations.ViewId;
import org.springframework.web.bind.annotation.GetMapping;

@ViewId("view_customers_by_email")
@Table("customers_by_email")
@Subscribe.ValueEntity(CustomerEntity.class)
public class CustomerByEmailView extends View<Customer> {

  @GetMapping("/customer/by_email/{email}")
  @Query("SELECT * FROM customers_by_email WHERE email = :email")
  public Customer getCustomer(String email) {
    return null;
  }
}
