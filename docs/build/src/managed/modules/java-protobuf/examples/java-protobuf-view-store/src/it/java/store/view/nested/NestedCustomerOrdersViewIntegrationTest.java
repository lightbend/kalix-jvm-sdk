package store.view.nested;

import kalix.javasdk.testkit.KalixTestKit;
import kalix.javasdk.testkit.junit.jupiter.KalixTestKitExtension;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import store.Main;
import store.customer.api.CustomerApi;
import store.customer.api.Customers;
import store.customer.domain.CustomerDomain;
import store.order.api.OrderApi;
import store.order.api.Orders;
import store.product.api.ProductApi;
import store.product.api.Products;
import store.product.domain.ProductDomain;

import static java.util.concurrent.TimeUnit.*;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class NestedCustomerOrdersViewIntegrationTest {

  @RegisterExtension
  public static final KalixTestKitExtension testKit =
      new KalixTestKitExtension(
          Main.createKalix(), KalixTestKit.Settings.DEFAULT.withAdvancedViews());

  private final Products products;
  private final Customers customers;
  private final Orders orders;
  private final NestedCustomerOrders view;

  public NestedCustomerOrdersViewIntegrationTest() {
    products = testKit.getGrpcClient(Products.class);
    customers = testKit.getGrpcClient(Customers.class);
    orders = testKit.getGrpcClient(Orders.class);
    view = testKit.getGrpcClient(NestedCustomerOrders.class);
  }

  private OrdersView.CustomerOrders getCustomerOrders(String customerId) throws Exception {
    return view.get(OrdersView.CustomerOrdersRequest.newBuilder().setCustomerId(customerId).build())
        .toCompletableFuture()
        .get(5, SECONDS);
  }

  @Test
  public void getCustomerOrders() throws Exception {
    ProductApi.Product product1 =
        ProductApi.Product.newBuilder()
            .setProductId("P123")
            .setProductName("Super Duper Thingamajig")
            .setPrice(
                ProductApi.Money.newBuilder().setCurrency("USD").setUnits(123).setCents(45).build())
            .build();
    products.create(product1).toCompletableFuture().get(5, SECONDS);
    ProductApi.Product product2 =
        ProductApi.Product.newBuilder()
            .setProductId("P987")
            .setProductName("Awesome Whatchamacallit")
            .setPrice(
                ProductApi.Money.newBuilder().setCurrency("NZD").setUnits(987).setCents(65).build())
            .build();
    products.create(product2).toCompletableFuture().get(5, SECONDS);
    CustomerApi.Customer customer =
        CustomerApi.Customer.newBuilder()
            .setCustomerId("C001")
            .setEmail("someone@example.com")
            .setName("Some Customer")
            .setAddress(
                CustomerApi.Address.newBuilder()
                    .setStreet("123 Some Street")
                    .setCity("Some City")
                    .build())
            .build();
    customers.create(customer).toCompletableFuture().get(5, SECONDS);
    OrderApi.Order order1 =
        OrderApi.Order.newBuilder()
            .setOrderId("O1234")
            .setProductId("P123")
            .setCustomerId("C001")
            .setQuantity(42)
            .build();
    orders.create(order1).toCompletableFuture().get(5, SECONDS);
    OrderApi.Order order2 =
        OrderApi.Order.newBuilder()
            .setOrderId("O5678")
            .setProductId("P987")
            .setCustomerId("C001")
            .setQuantity(7)
            .build();
    orders.create(order2).toCompletableFuture().get(5, SECONDS);

    // wait until the view is eventually updated
    await()
        .ignoreExceptions()
        .atMost(20, SECONDS)
        .pollInterval(500, MILLISECONDS)
        .until(() -> getCustomerOrders("C001").getOrdersCount() >= 2);

    OrdersView.CustomerOrders result = getCustomerOrders("C001");
    assertEquals(2, result.getOrdersCount());
    OrdersView.CustomerOrders expectedOrders =
        OrdersView.CustomerOrders.newBuilder()
            .setCustomerId("C001")
            .setEmail("someone@example.com")
            .setName("Some Customer")
            .setAddress(
                CustomerDomain.Address.newBuilder()
                    .setStreet("123 Some Street")
                    .setCity("Some City")
                    .build())
            .addOrders(
                OrdersView.CustomerOrder.newBuilder()
                    .setCustomerId("C001")
                    .setOrderId("O1234")
                    .setProductId("P123")
                    .setProductName("Super Duper Thingamajig")
                    .setPrice(
                        ProductDomain.Money.newBuilder()
                            .setCurrency("USD")
                            .setUnits(123)
                            .setCents(45)
                            .build())
                    .setQuantity(42)
                    .setCreated(result.getOrders(0).getCreated())
                    .build())
            .addOrders(
                OrdersView.CustomerOrder.newBuilder()
                    .setCustomerId("C001")
                    .setOrderId("O5678")
                    .setProductId("P987")
                    .setProductName("Awesome Whatchamacallit")
                    .setPrice(
                        ProductDomain.Money.newBuilder()
                            .setCurrency("NZD")
                            .setUnits(987)
                            .setCents(65)
                            .build())
                    .setQuantity(7)
                    .setCreated(result.getOrders(1).getCreated()))
            .build();
    assertEquals(expectedOrders, result);
  }
}
