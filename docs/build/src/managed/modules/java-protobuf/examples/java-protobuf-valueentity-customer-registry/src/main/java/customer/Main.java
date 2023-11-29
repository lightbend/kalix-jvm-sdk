package customer;

import customer.action.CustomerActionImpl;
import customer.domain.CustomerValueEntity;
import customer.view.CustomerByEmailView;
import customer.view.CustomerByNameView;
import customer.view.CustomerDetailsByNameView;
import customer.view.CustomerSummaryByNameView;
import customer.view.CustomersResponseByCityView;
import customer.view.CustomersResponseByNameView;
import kalix.javasdk.Kalix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public final class Main {

  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  // tag::register[]
  public static Kalix createKalix() {
    // end::register[]
    // The KalixFactory automatically registers any generated Actions, Views or Entities,
    // and is kept up-to-date with any changes in your protobuf definitions.
    // If you prefer, you may remove this and manually register these components in a
    // `new Kalix()` instance.
    // tag::register[]
    return KalixFactory.withComponents(
      CustomerValueEntity::new,
      CustomerActionImpl::new,
      CustomerByEmailView::new,
      CustomerByNameView::new,
      CustomerDetailsByNameView::new,
      CustomerSummaryByNameView::new,
        CustomersResponseByCityView::new,
      CustomersResponseByNameView::new);
  }
  // end::register[]

  public static void main(String[] args) throws Exception {
    LOG.info("starting the Kalix service");
    createKalix().start();
  }
}
