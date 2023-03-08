package com.example.view;

import com.example.api.CustomerEntity;
import com.example.api.CustomerSummary;
import com.example.domain.Customer;
import kalix.javasdk.view.View;
import kalix.springsdk.annotations.Query;
import kalix.springsdk.annotations.Subscribe;
import kalix.springsdk.annotations.Table;
import kalix.springsdk.annotations.ViewId;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Flux;

@ViewId("view_stream_customers_by_name")
// tag::class[]
@Table("customers")
@Subscribe.ValueEntity(CustomerEntity.class)
public class CustomersStreamByName extends View<Customer> { // <1>

  @GetMapping("/summary/by_name/{customerName}")   // <2>
  @Query(
      value = """
        SELECT customerId AS id, name
          FROM customers
          WHERE name = :customerName
        """, // <3>
      streamUpdates = true) // <4>
  public Flux<CustomerSummary> getCustomer() { // <5>
    return null;
  }
}
// end::class[]
