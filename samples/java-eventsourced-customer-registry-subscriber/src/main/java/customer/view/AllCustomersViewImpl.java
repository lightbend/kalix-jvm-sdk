package customer.view;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import customer.api.CustomerApi;
import customer.api.PublisherApi;
import kalix.javasdk.view.View;
import kalix.javasdk.view.ViewContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

// This class was initially generated based on the .proto definition by Kalix tooling.
// This is the implementation for the View Service described in your customer/view/customer_view.proto file.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class AllCustomersViewImpl extends AbstractAllCustomersView {

  private static Logger log = LoggerFactory.getLogger(AllCustomersViewImpl.class);

  public AllCustomersViewImpl(ViewContext context) {}

  @Override
  public CustomerApi.Customer emptyState() {
    return CustomerApi.Customer.getDefaultInstance();
  }

  @Override
  public View.UpdateEffect<CustomerApi.Customer> processCustomerCreated(
    CustomerApi.Customer state, PublisherApi.Created created) {
    log.info("Customer {} created: {}", updateContext().eventSubject(), created);
    Timestamp now = Timestamps.fromMillis(Instant.now().toEpochMilli());
    return effects().updateState(CustomerApi.Customer.newBuilder()
        .setCustomerId(created.getCustomerId())
        .setName(created.getCustomerName())
        .setEmail(created.getEmail())
        .setUpdates(1)
        .setCreated(now)
        .setLastUpdate(now)
        .build());
  }
  @Override
  public View.UpdateEffect<CustomerApi.Customer> processCustomerNameChanged(
    CustomerApi.Customer state, PublisherApi.NameChanged nameChanged) {
    log.info("Customer {} name changed: {}", updateContext().eventSubject(), nameChanged);

    Timestamp now = Timestamps.fromMillis(Instant.now().toEpochMilli());
    return effects().updateState(state.toBuilder()
        .setName(nameChanged.getCustomerName())
        .setUpdates(1)
        .setLastUpdate(now)
        .build());
  }
}

