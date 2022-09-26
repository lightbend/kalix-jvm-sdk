/* This code was generated by Kalix tooling.
 * As long as this file exists it will not be re-generated.
 * You are free to make changes to this file.
 */
package customer.view;

import kalix.javasdk.testkit.junit.KalixTestKitResource;
import customer.Main;
import customer.api.CustomerApi;
import customer.api.CustomerService;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

// Example of an integration test calling our service via the Kalix proxy
// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class CustomerByEmailIntegrationTest {

  /**
   * The test kit starts both the service container and the Kalix proxy.
   */
  @ClassRule
  public static final KalixTestKitResource testKit =
          new KalixTestKitResource(Main.createKalix());

  /**
   * Use the generated gRPC client to call the service through the Kalix proxy.
   */
  private final CustomerService apiClient;
  private final CustomerByEmail viewClient;

  public CustomerByEmailIntegrationTest() {
    apiClient = testKit.getGrpcClient(CustomerService.class);
    viewClient = testKit.getGrpcClient(CustomerByEmail.class);
  }

  @Test
  public void findByEmail() throws Exception {
    String id = UUID.randomUUID().toString();
    apiClient.create(CustomerApi.Customer.newBuilder()
            .setCustomerId(id)
            .setName("Johanna")
            .setEmail("foo@example.com")
            .build())
        .toCompletableFuture()
        .get(5, SECONDS);

    CustomerViewModel.ByEmailRequest req =
        CustomerViewModel.ByEmailRequest.newBuilder().setEmail("foo@example.com").build();

    // // the view is eventually updated
    await()
      .ignoreExceptions()
      .atMost(20, SECONDS)
      .until(() -> viewClient.getCustomer(req).toCompletableFuture()
      .get(3, SECONDS).getCustomerId().equals(id));
  }

}
