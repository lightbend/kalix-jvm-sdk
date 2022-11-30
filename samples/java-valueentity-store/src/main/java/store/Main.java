package store;

import kalix.javasdk.Kalix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import store.customer.domain.CustomerValueEntity;
import store.order.domain.OrderValueEntity;
import store.product.domain.ProductValueEntity;
import store.view.joined.JoinedCustomerOrdersView;
import store.view.nested.NestedCustomerOrdersView;
import store.view.structured.StructuredCustomerOrdersView;

public final class Main {

  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  public static Kalix createKalix() {
    return KalixFactory.withComponents(
      CustomerValueEntity::new,
      OrderValueEntity::new,
      ProductValueEntity::new,
      JoinedCustomerOrdersView::new,
      NestedCustomerOrdersView::new,
      StructuredCustomerOrdersView::new);
  }

  public static void main(String[] args) throws Exception {
    LOG.info("Starting the Kalix store sample");
    createKalix().start();
  }
}
