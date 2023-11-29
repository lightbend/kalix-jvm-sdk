package store.order.api;

import kalix.javasdk.testkit.KalixTestKit;
import kalix.javasdk.testkit.junit.jupiter.KalixTestKitExtension;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import store.Main;

import static java.util.concurrent.TimeUnit.*;
import static org.junit.jupiter.api.Assertions.*;

// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class OrderEntityIntegrationTest {

  @RegisterExtension
  public static final KalixTestKitExtension testKit =
      new KalixTestKitExtension(
          Main.createKalix(), KalixTestKit.Settings.DEFAULT.withAdvancedViews());

  private final Orders orders;

  public OrderEntityIntegrationTest() {
    orders = testKit.getGrpcClient(Orders.class);
  }

  @Test
  public void createAndGetEntity() throws Exception {
    OrderApi.Order order =
        OrderApi.Order.newBuilder()
            .setOrderId("O1234")
            .setProductId("P123")
            .setCustomerId("C001")
            .setQuantity(42)
            .build();
    orders.create(order).toCompletableFuture().get(5, SECONDS);
    OrderApi.Order result =
        orders
            .get(OrderApi.GetOrder.newBuilder().setOrderId("O1234").build())
            .toCompletableFuture()
            .get(5, SECONDS);
    assertEquals(order, result);
  }
}
