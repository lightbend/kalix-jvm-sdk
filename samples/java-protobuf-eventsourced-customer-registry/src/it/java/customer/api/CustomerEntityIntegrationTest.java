package customer.api;

import com.google.protobuf.Empty;
import customer.Main;
import customer.domain.CustomerDomain;
import customer.view.CustomerByNameView;
import kalix.javasdk.testkit.EventSourcedResult;
import kalix.javasdk.testkit.junit.jupiter.KalixTestKitExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.concurrent.TimeUnit.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

// Example of an integration test calling our service via the Kalix Runtime
// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class CustomerEntityIntegrationTest {

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

  public CustomerEntityIntegrationTest() {
    client = testKit.getGrpcClient(CustomerService.class);
  }

  @Test
  public void runAll() throws Exception {
    CustomerBatchProcessor processor = new CustomerBatchProcessor(client);

      List<CustomerBatchProcessor.CustomerData> customerDataList = new ArrayList<>();
      // Add many customers programmatically
      for (int i = 1; i <= 10; i++) {
        CustomerBatchProcessor.CustomerData data = new CustomerBatchProcessor.CustomerData();
        data.setCustomerId("cust-" + i);
        data.setInitialName("Customer " + i % 10);
        data.setEmail("customer" + i + "@example.com");
        data.setUpdatedName("Customer " + (i % 10));
        customerDataList.add(data);
      }

      processor.processBatch(customerDataList);
      Thread.sleep(100000);
//      testKit.getGrpcClient(CustomerByNameView.class)

  }
  @Test
  public void testCreate() throws Exception {
    // TODO: set fields in command, and provide assertions to match replies
    CustomerApi.Customer customer1  = CustomerApi.Customer
            .newBuilder()
            .setCustomerId("aid")
            .setName("a-name")
            .setEmail("a@gmail.com")
            .build();
     client.create(customer1)
             .toCompletableFuture().get(3, SECONDS);
    CustomerApi.ChangeNameRequest  changeNameRequest = CustomerApi.ChangeNameRequest
            .newBuilder()
            .setCustomerId("aid")
            .setNewName("aa-name")
            .build();
     client.changeName(changeNameRequest).toCompletableFuture().get(3, SECONDS);
  }

}
