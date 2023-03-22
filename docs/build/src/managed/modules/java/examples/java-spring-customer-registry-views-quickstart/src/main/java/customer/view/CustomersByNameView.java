package customer.view;

// tag::class[]
import customer.api.CustomerEntity;
import customer.domain.Customer;
import kalix.javasdk.annotations.Query;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.annotations.Table;
import kalix.javasdk.annotations.ViewId;
import kalix.javasdk.view.View;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Flux;

@ViewId("view_customers_by_name")
@Table("customers_by_name")
public class CustomersByNameView
  extends View<CustomersByNameView.CustomerSummary> { // <1>

  public record CustomerSummary(String name, String email) { // <2>
  }

  @GetMapping("/customers/by_name/{customer_name}")
  @Query("SELECT * FROM customers_by_name WHERE name = :customer_name")
  public Flux<CustomerSummary> getCustomers(String name) { // <3>
    return null;
  }

  @Subscribe.ValueEntity(CustomerEntity.class) // <4>
  public UpdateEffect<CustomerSummary> onUpdate(Customer customer) {
    return effects()
      .updateState(new CustomerSummary(customer.name(), customer.email()));
  }
}
// end::class[]
