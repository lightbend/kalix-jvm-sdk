package customer.view;

import customer.Main;
import customer.domain.CustomerDomain.Address;
import customer.domain.CustomerDomain.CustomerState;
import customer.view.CustomerViewModel.ByCityRequest;
import kalix.javasdk.testkit.EventingTestKit;
import kalix.javasdk.testkit.junit.jupiter.KalixTestKitExtension;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static kalix.javasdk.testkit.KalixTestKit.Settings.DEFAULT;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

// tag::view-test[]
public class CustomersResponseByCityViewIntegrationTest {

  @RegisterExtension
  public static final KalixTestKitExtension testKit =
      new KalixTestKitExtension(
          Main.createKalix(),
          DEFAULT.withValueEntityIncomingMessages("customer")); // <1>

  private final CustomersResponseByCity viewClient;

  public CustomersResponseByCityViewIntegrationTest() {
    viewClient = testKit.getGrpcClient(CustomersResponseByCity.class);
  }

  @Test
  public void shouldFindCustomersByCity() {
    EventingTestKit.IncomingMessages customerEvents = testKit.getValueEntityIncomingMessages("customer"); // <2>

    CustomerState johanna = CustomerState.newBuilder().setCustomerId("1")
        .setEmail("johanna@example.com")
        .setName("Johanna")
        .setAddress(Address.newBuilder().setStreet("Cool Street").setCity("Porto").build())
        .build();

    CustomerState bob = CustomerState.newBuilder().setCustomerId("1")
        .setEmail("bob@example.com")
        .setName("Bob")
        .setAddress(Address.newBuilder().setStreet("Baker Street").setCity("London").build())
        .build();

    CustomerState alice = CustomerState.newBuilder().setCustomerId("1")
        .setEmail("alice@example.com")
        .setName("Alice")
        .setAddress(Address.newBuilder().setStreet("Long Street").setCity("Wroclaw").build())
        .build();

    customerEvents.publish(johanna, "1"); // <3>
    customerEvents.publish(bob, "2");
    customerEvents.publish(alice, "3");

    await()
        .ignoreExceptions()
        .atMost(5, TimeUnit.SECONDS)
        .untilAsserted(() -> {
              ByCityRequest byCities = ByCityRequest.newBuilder().addAllCities(List.of("Porto", "London")).build();

              CustomerViewModel.CustomersResponse customersResponse = viewClient
                  .getCustomers(byCities) // <4>
                  .toCompletableFuture().get(2, TimeUnit.SECONDS);

              assertTrue(customersResponse.getCustomersList().containsAll(List.of(johanna, bob)));
            }
        );
  }
}
// end::view-test[]