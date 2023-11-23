package store.customer.api;

import kalix.javasdk.testkit.KalixTestKit;
import kalix.javasdk.testkit.junit.jupiter.KalixTestKitExtension;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import store.Main;

import static java.util.concurrent.TimeUnit.*;
import static org.junit.jupiter.api.Assertions.*;

// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class CustomerEntityIntegrationTest {

  @RegisterExtension
  public static final KalixTestKitExtension testKit =
      new KalixTestKitExtension(
          Main.createKalix(), KalixTestKit.Settings.DEFAULT.withAdvancedViews());

  private final Customers customers;

  public CustomerEntityIntegrationTest() {
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

  @Test
  public void changeCustomerName() throws Exception {
    CustomerApi.Customer customer =
        CustomerApi.Customer.newBuilder()
            .setCustomerId("C002")
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
            .get(CustomerApi.GetCustomer.newBuilder().setCustomerId("C002").build())
            .toCompletableFuture()
            .get(5, SECONDS);
    assertEquals(customer, result);

    String newName = "Some Name";
    CustomerApi.ChangeCustomerName changeCustomerName =
        CustomerApi.ChangeCustomerName.newBuilder()
            .setCustomerId("C002")
            .setNewName(newName)
            .build();
    customers.changeName(changeCustomerName).toCompletableFuture().get(5, SECONDS);
    CustomerApi.Customer customerWithNewName = customer.toBuilder().setName(newName).build();
    CustomerApi.Customer updatedResult =
        customers
            .get(CustomerApi.GetCustomer.newBuilder().setCustomerId("C002").build())
            .toCompletableFuture()
            .get(5, SECONDS);
    assertEquals(customerWithNewName, updatedResult);
  }

  @Test
  public void changeCustomerAddress() throws Exception {
    CustomerApi.Customer customer =
        CustomerApi.Customer.newBuilder()
            .setCustomerId("C003")
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
            .get(CustomerApi.GetCustomer.newBuilder().setCustomerId("C003").build())
            .toCompletableFuture()
            .get(5, SECONDS);
    assertEquals(customer, result);

    CustomerApi.Address newAddress =
        CustomerApi.Address.newBuilder()
            .setStreet("42 Some Road")
            .setCity("Some Other City")
            .build();
    CustomerApi.ChangeCustomerAddress changeCustomerAddress =
        CustomerApi.ChangeCustomerAddress.newBuilder()
            .setCustomerId("C003")
            .setNewAddress(newAddress)
            .build();
    customers.changeAddress(changeCustomerAddress).toCompletableFuture().get(5, SECONDS);
    CustomerApi.Customer customerWithNewAddress =
        customer.toBuilder().setAddress(newAddress).build();
    CustomerApi.Customer updatedResult =
        customers
            .get(CustomerApi.GetCustomer.newBuilder().setCustomerId("C003").build())
            .toCompletableFuture()
            .get(5, SECONDS);
    assertEquals(customerWithNewAddress, updatedResult);
  }
}
