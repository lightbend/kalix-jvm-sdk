package customer.view;

import customer.api.CustomerEntity;
import customer.api.CustomersResponse;
import customer.domain.Customer;
import kalix.javasdk.view.View;
import kalix.javasdk.annotations.Query;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.annotations.Table;
import kalix.javasdk.annotations.ViewId;
import org.springframework.web.bind.annotation.GetMapping;

@ViewId("view_response_customers_by_name")
@Table("customers_by_name")
// tag::class[]
@Subscribe.ValueEntity(CustomerEntity.class)
public class CustomersResponseByName extends View<Customer> { // <1>

  @GetMapping("/wrapped/by_name/{customerName}")   // <2>
  @Query("""
    SELECT * AS customers
      FROM customers_by_name
      WHERE name = :customerName
    """) // <3>
  public CustomersResponse getCustomers() { // <4>
    return null;
  }
}
// end::class[]
