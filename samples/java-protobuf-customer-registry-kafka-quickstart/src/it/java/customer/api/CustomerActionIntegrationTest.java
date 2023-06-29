package customer.api;

import com.google.protobuf.Empty;
import customer.Main;
import kalix.javasdk.testkit.EventingTestKit;
import kalix.javasdk.testkit.KalixTestKit;
import kalix.javasdk.testkit.junit.KalixTestKitResource;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static kalix.javasdk.testkit.KalixTestKit.Settings.EventingSupport.GOOGLE_PUBSUB;
import static org.junit.Assert.assertEquals;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

// Example of an integration test calling our service via the Kalix proxy
// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class CustomerActionIntegrationTest {

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

  private final EventingTestKit.Topic outTopic;

  public CustomerActionIntegrationTest() {
    client = testKit.getGrpcClient(CustomerService.class);
    outTopic = testKit.getTopic("customer_changes");
  }

  @Test
  public void createAndPublish() throws Exception {
    var id = UUID.randomUUID().toString();
    var customer = buildCustomer(id, "Johanna", "foo@example.com", "Porto", "Long Road");
    createCustomer(customer);

    // wait for action to publish the change of state
    var createdMsgOut = outTopic.expectOneTyped(CustomerApi.Customer.class);
    var metadata = createdMsgOut.getMetadata();

    assertEquals(Optional.of("customer.api.Customer"), metadata.get("ce-type"));
    assertEquals(Optional.of("application/protobuf"), metadata.get("Content-Type"));
    assertEquals(noAddress(customer), createdMsgOut.getPayload());

    //change customer name
    var completeName = "Johanna Doe";
    client.changeName(CustomerApi.ChangeNameRequest.newBuilder()
            .setCustomerId(id)
            .setNewName(completeName)
            .build())
        .toCompletableFuture()
        .get(5, SECONDS);

    // wait for action to publish the change of state
    var nameChangeMsgOut = outTopic.expectOneTyped(CustomerApi.Customer.class);
    assertEquals(completeName, nameChangeMsgOut.getPayload().getName());

    outTopic.expectNone(); // no more messages are sent
  }

  private CustomerApi.Customer buildCustomer(String id, String name, String email, String city, String street) {
    return CustomerApi.Customer.newBuilder()
        .setCustomerId(id)
        .setName(name)
        .setEmail(email)
        .setAddress(CustomerApi.Address.newBuilder()
            .setCity(city)
            .setStreet(street)
            .build())
        .build();
  }
  private Empty createCustomer(CustomerApi.Customer toCreate) throws ExecutionException, InterruptedException, TimeoutException {
    return client.create(toCreate)
        .toCompletableFuture()
        .get(5, SECONDS);
  }

  private CustomerApi.Customer noAddress(CustomerApi.Customer customer) {
    return CustomerApi.Customer.newBuilder(customer)
        .clearAddress()
        .build();
  }

}
