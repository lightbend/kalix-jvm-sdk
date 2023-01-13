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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;

import java.sql.Timestamp;
import java.time.Instant;

@ViewId("view_summary_customer_by_ts")
@Table("customers")
public class CustomerSummaryByTimestamp extends View<CustomerSummary> {

  @Subscribe.ValueEntity(CustomerEntity.class)
  public UpdateEffect<CustomerSummary> onChange(Customer customer) {
    return effects()
        .updateState(new CustomerSummary(customer.email(), customer.name(), customer.createdAt()));
  }

  @GetMapping("/summary/created_after")
  @Query("SELECT * FROM customers ORDER BY createdAt")
  public Flux<CustomerSummary> getCustomer() { // <7>
    return null;
  }
}
