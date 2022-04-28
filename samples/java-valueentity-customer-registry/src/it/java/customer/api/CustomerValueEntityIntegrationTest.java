package customer.api;

import kalix.javasdk.testkit.junit.KalixTestKitResource;
import com.google.protobuf.Empty;
import customer.Main;
import customer.domain.CustomerDomain;
import org.junit.ClassRule;
import org.junit.Test;

import static java.util.concurrent.TimeUnit.*;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

// Example of an integration test calling our service via the Kalix proxy
// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class CustomerValueEntityIntegrationTest {

  /**
   * The test kit starts both the service container and the Kalix proxy.
   */
  @ClassRule
  public static final KalixTestKitResource testKit =
    new KalixTestKitResource(Main.createKalix());

  /**
   * Use the generated gRPC client to call the service through the Kalix proxy.
   */
  private final CustomerService client;

  public CustomerValueEntityIntegrationTest() {
    client = testKit.getGrpcClient(CustomerService.class);
  }

  @Test
  public void createOnNonExistingEntity() throws Exception {
    // TODO: set fields in command, and provide assertions to match replies
    // client.create(CustomerApi.Customer.newBuilder().build())
    //         .toCompletableFuture().get(5, SECONDS);
  }

  @Test
  public void changeNameOnNonExistingEntity() throws Exception {
    // TODO: set fields in command, and provide assertions to match replies
    // client.changeName(CustomerApi.ChangeNameRequest.newBuilder().build())
    //         .toCompletableFuture().get(5, SECONDS);
  }

  @Test
  public void changeAddressOnNonExistingEntity() throws Exception {
    // TODO: set fields in command, and provide assertions to match replies
    // client.changeAddress(CustomerApi.ChangeAddressRequest.newBuilder().build())
    //         .toCompletableFuture().get(5, SECONDS);
  }

  @Test
  public void getCustomerOnNonExistingEntity() throws Exception {
    // TODO: set fields in command, and provide assertions to match replies
    // client.getCustomer(CustomerApi.GetCustomerRequest.newBuilder().build())
    //         .toCompletableFuture().get(5, SECONDS);
  }
}
