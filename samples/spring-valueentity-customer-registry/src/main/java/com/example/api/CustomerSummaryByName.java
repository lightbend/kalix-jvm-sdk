package com.example.api;

// tag::class[]
import com.example.domain.Customer;
import kalix.javasdk.view.View;
import kalix.springsdk.annotations.Query;
import kalix.springsdk.annotations.Subscribe;
import kalix.springsdk.annotations.Table;
import kalix.springsdk.annotations.ViewId;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Flux;

@ViewId("view_summary_customers_by_name")
@Table("customers_by_name")
public class CustomerSummaryByName extends View<CustomerSummary> { // <1>

  @Subscribe.ValueEntity(CustomerEntity.class) // <2>
  public UpdateEffect<CustomerSummary> onChange(Customer customer) {
    return effects()
        .updateState(new CustomerSummary(customer.email(), customer.name()));
  }

  @GetMapping("/summary/by_name/{customer_name}")   // <2>
  @Query(value = "SELECT * FROM customers_by_name WHERE name = :customer_name", streamUpdates = true) // <2>
  public Flux<CustomerSummary> getCustomer() {
    return null; // <3>
  }
}
// end::class[]
