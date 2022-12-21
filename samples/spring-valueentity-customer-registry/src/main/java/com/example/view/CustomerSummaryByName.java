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

@ViewId("view_summary_customer_by_name")
// tag::class[]
@Table("customers")
public class CustomerSummaryByName extends View<CustomerSummary> { // <1>

  @Subscribe.ValueEntity(CustomerEntity.class) // <2>
  public UpdateEffect<CustomerSummary> onChange(Customer customer) { // <3>
    return effects()
        .updateState(new CustomerSummary(customer.email(), customer.name())); // <4>
  }
  // end::class[]

  // tag::delete[]
  @Subscribe.ValueEntity(value = CustomerEntity.class, handleDeletes = true) // <1>
  public UpdateEffect<CustomerSummary> onDelete() { // <2>
    return effects()
        .deleteEntity(); // <3>
  }
  // end::delete[]

  // tag::class[]
  @GetMapping("/summary/by_name/{customerName}")   // <5>
  @Query("SELECT * FROM customers WHERE name = :customerName") // <6>
  public CustomerSummary getCustomer() { // <7>
    return null;
  }
}
// end::class[]
