package com.example.api;

// tag::class[]
import com.example.domain.Customer;
import kalix.javasdk.view.View;
import kalix.springsdk.annotations.Query;
import kalix.springsdk.annotations.Subscribe;
import kalix.springsdk.annotations.Table;
import kalix.springsdk.annotations.ViewId;
import org.springframework.web.bind.annotation.GetMapping;

@ViewId("view_response_customers_by_name")
@Table("customers_by_name")
@Subscribe.ValueEntity(CustomerEntity.class)
public class CustomersResponseByName extends View<Customer> { // <1>

  @GetMapping("/wrapped/by_name/{customer_name}")   // <2>
  @Query("SELECT * AS results FROM customers_by_name WHERE name = :customer_name") // <3>
  public CustomersResponse getCustomer() {
    return null;
  }
}
// end::class[]
