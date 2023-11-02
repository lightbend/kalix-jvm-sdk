package customer.api;

import customer.Main;
import customer.views.Customer;
import customer.views.CustomerPublicEvent.Created;
import kalix.javasdk.testkit.EventingTestKit.IncomingMessages;
import kalix.javasdk.testkit.KalixTestKit;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = Main.class)
@Import(TestKitConfig.class)
public class CustomersByNameViewIntegrationTest extends KalixIntegrationTestKitSupport {

  @Autowired
  private KalixTestKit kalixTestKit;
  @Autowired
  private WebClient webClient;

  @Test
  public void shouldReturnCustomerByName() {
    IncomingMessages customerEvents = kalixTestKit.getStreamIncomingMessages("customer-registry", "customer_events");

    String bob = "bob";
    Created created1 = new Created("bob@gmail.com", bob);
    Created created2 = new Created("alice@gmail.com", "alice");

    customerEvents.publish(created1, "b");
    customerEvents.publish(created2, "a");

    //then
    await()
        .ignoreExceptions()
        .atMost(20, TimeUnit.SECONDS)
        .pollInterval(1, TimeUnit.SECONDS)
        .untilAsserted(() -> {
              Customer customer = webClient.get()
                  .uri("/customers/by_name/" + bob)
                  .retrieve()
                  .bodyToFlux(Customer.class)
                  .blockFirst(timeout);

              assertThat(customer).isEqualTo(new Customer("b", created1.email(), created1.name()));
            }
        );
  }
}
