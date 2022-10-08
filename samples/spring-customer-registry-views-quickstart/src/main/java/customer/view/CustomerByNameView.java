package customer.view;

import customer.api.Customer;
import customer.api.CustomerEntity;
import kalix.javasdk.view.View;
import kalix.springsdk.annotations.Query;
import kalix.springsdk.annotations.Subscribe;
import kalix.springsdk.annotations.Table;
import org.springframework.web.bind.annotation.GetMapping;

@Table("customers_by_name")
@Subscribe.ValueEntity(CustomerEntity.class)
public class CustomerByNameView extends View<Customer> {

  @GetMapping("/customer/by_name/{customer_name}")
  @Query("SELECT * FROM customers_by_name WHERE name = :customer_name")
  public Customer getCustomer(String name) {
    return null;
  }
}
