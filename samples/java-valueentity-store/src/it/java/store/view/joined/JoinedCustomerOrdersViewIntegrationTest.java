package store.view.joined;

import akka.stream.javadsl.Sink;
import kalix.javasdk.testkit.KalixTestKit;
import kalix.javasdk.testkit.junit.KalixTestKitResource;
import org.junit.ClassRule;
import org.junit.Test;
import store.Main;
import store.customer.api.CustomerApi;
import store.customer.api.Customers;
import store.customer.domain.CustomerDomain;
import store.order.api.OrderApi;
import store.order.api.Orders;
import store.product.api.ProductApi;
import store.product.api.Products;
import store.product.domain.ProductDomain;

import java.util.List;

import static java.util.concurrent.TimeUnit.*;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.*;

// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class JoinedCustomerOrdersViewIntegrationTest {

  @ClassRule
  // tag::testkit-advanced-views[]
  public static final KalixTestKitResource testKit =
      new KalixTestKitResource(
          Main.createKalix(), KalixTestKit.Settings.DEFAULT.withAdvancedViews());
  // end::testkit-advanced-views[]

  private final Products products;
  private final Customers customers;
  private final Orders orders;
  private final JoinedCustomerOrders view;

  public JoinedCustomerOrdersViewIntegrationTest() {
    products = testKit.getGrpcClient(Products.class);
    customers = testKit.getGrpcClient(Customers.class);
    orders = testKit.getGrpcClient(Orders.class);
    view = testKit.getGrpcClient(JoinedCustomerOrders.class);
  }

  private List<OrdersView.CustomerOrder> getCustomerOrders(String customerId) throws Exception {
    return view.get(OrdersView.CustomerOrdersRequest.newBuilder().setCustomerId(customerId).build())
        .runWith(Sink.seq(), testKit.getMaterializer())
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
        .until(() -> getCustomerOrders("C001").size() >= 2);

    List<OrdersView.CustomerOrder> results = getCustomerOrders("C001");
    assertEquals(2, results.size());
    OrdersView.CustomerOrder expectedOrder1 =
        OrdersView.CustomerOrder.newBuilder()
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
            .setCustomerId("C001")
            .setEmail("someone@example.com")
            .setName("Some Customer")
            .setAddress(
                CustomerDomain.Address.newBuilder()
                    .setStreet("123 Some Street")
                    .setCity("Some City")
                    .build())
            .setCreated(results.get(0).getCreated())
            .build();
    assertEquals(expectedOrder1, results.get(0));
    OrdersView.CustomerOrder expectedOrder2 =
        OrdersView.CustomerOrder.newBuilder()
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
            .setCustomerId("C001")
            .setEmail("someone@example.com")
            .setName("Some Customer")
            .setAddress(
                CustomerDomain.Address.newBuilder()
                    .setStreet("123 Some Street")
                    .setCity("Some City")
                    .build())
            .setCreated(results.get(1).getCreated())
            .build();
    assertEquals(expectedOrder2, results.get(1));
  }
}
