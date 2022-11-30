package store.customer.api;

import kalix.javasdk.testkit.KalixTestKit;
import kalix.javasdk.testkit.junit.KalixTestKitResource;
import org.junit.ClassRule;
import org.junit.Test;
import store.Main;

import static java.util.concurrent.TimeUnit.*;
import static org.junit.Assert.*;

// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class CustomerValueEntityIntegrationTest {

  @ClassRule
  public static final KalixTestKitResource testKit =
      new KalixTestKitResource(
          Main.createKalix(), KalixTestKit.Settings.DEFAULT.withAdvancedViews());

  private final Customers customers;

  public CustomerValueEntityIntegrationTest() {
    customers = testKit.getGrpcClient(Customers.class);
  }

  @Test
  public void createAndGetEntity() throws Exception {
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
    CustomerApi.Customer result =
        customers
            .get(CustomerApi.GetCustomer.newBuilder().setCustomerId("C001").build())
            .toCompletableFuture()
            .get(5, SECONDS);
    assertEquals(customer, result);
  }
}
