package customer.api;

import java.util.UUID;
import kalix.javasdk.testkit.junit.jupiter.KalixTestKitExtension;
import customer.Main;
import customer.api.CustomerApi;
import customer.api.CustomerService;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

// Example of an integration test calling our service via the Kalix Runtime
// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class CustomerIntegrationTest {

  /**
   * The test kit starts both the service container and the Kalix Runtime.
   */
  @RegisterExtension
  public static final KalixTestKitExtension testKit =
          new KalixTestKitExtension(Main.createKalix());

  /**
   * Use the generated gRPC client to call the service through the Kalix Runtime.
   */
  private final CustomerService client;

  public CustomerIntegrationTest() {
    client = testKit.getGrpcClient(CustomerService.class);
  }

  CustomerApi.Customer getCustomer(String customerId) throws Exception {
    return client
            .getCustomer(CustomerApi.GetCustomerRequest.newBuilder().setCustomerId(customerId).build())
            .toCompletableFuture()
            .get(5, SECONDS);
  }

  @Test
  public void create() throws Exception {
    String id = UUID.randomUUID().toString();
    client.create(CustomerApi.Customer.newBuilder()
                    .setCustomerId(id)
                    .setName("Johanna")
                    .setEmail("foo@example.com")
                    .build())
            .toCompletableFuture()
            .get(5, SECONDS);
    assertEquals("Johanna", getCustomer(id).getName());
  }

  @Test
  public void changeName() throws Exception {
    String id = UUID.randomUUID().toString();
    client.create(CustomerApi.Customer.newBuilder()
                    .setCustomerId(id)
                    .setName("Johanna")
                    .setEmail("foo@example.com")
                    .build())
            .toCompletableFuture()
            .get(5, SECONDS);
    client.changeName(CustomerApi.ChangeNameRequest.newBuilder()
                    .setCustomerId(id)
                    .setNewName("Katarina")
                    .build())
            .toCompletableFuture()
            .get(5, SECONDS);
    assertEquals("Katarina", getCustomer(id).getName());
  }

  @Test
  public void changeAddress() throws Exception {
    String id = UUID.randomUUID().toString();
    client.create(CustomerApi.Customer.newBuilder()
                    .setCustomerId(id)
                    .setName("Johanna")
                    .setEmail("foo@example.com")
                    .build())
            .toCompletableFuture()
            .get(5, SECONDS);
    client.changeAddress(CustomerApi.ChangeAddressRequest.newBuilder()
                    .setCustomerId(id)
                    .setNewAddress(
                            CustomerApi.Address.newBuilder()
                                    .setStreet("Elm st. 5")
                                    .setCity("New Orleans")
                                    .build()
                    )
                    .build())
            .toCompletableFuture()
            .get(5, SECONDS);
    assertEquals("Elm st. 5", getCustomer(id).getAddress().getStreet());
  }
}
