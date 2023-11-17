package customer.view;

import customer.Main;
import customer.api.CustomersResponse;
import customer.domain.Address;
import customer.domain.Customer;
import kalix.javasdk.client.ComponentClient;
import kalix.javasdk.testkit.EventingTestKit.IncomingMessages;
import kalix.javasdk.testkit.KalixTestKit;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

// tag::view-test[]
@Configuration
class TestKitConfig {

  @Bean
  @Profile("view-it-test")
  public KalixTestKit.Settings settings() {
    return KalixTestKit.Settings.DEFAULT
        .withValueEntityIncomingMessages("customer"); // <1>
  }
}

@SpringBootTest(classes = Main.class)
@Import(TestKitConfig.class)
@ActiveProfiles("view-it-test")
class CustomersResponseByCityIntegrationTest extends KalixIntegrationTestKitSupport {

  @Autowired
  private KalixTestKit kalixTestKit;
  @Autowired
  private ComponentClient componentClient;

  @Test
  public void shouldGetCustomerByCity() {
    IncomingMessages customerEvents = kalixTestKit.getValueEntityIncomingMessages("customer"); // <2>

    Customer johanna = new Customer("1", "johanna@example.com", "Johanna",
        new Address("Cool Street", "Porto"));
    Customer bob = new Customer("2", "boc@example.com", "Bob",
        new Address("Baker Street", "London"));
    Customer alice = new Customer("3", "alice@example.com", "Alice",
        new Address("Long Street", "Wroclaw"));


    customerEvents.publish(johanna, "1"); // <3>
    customerEvents.publish(bob, "2");
    customerEvents.publish(alice, "3");

    await()
        .ignoreExceptions()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> {

              CustomersResponse customersResponse = componentClient.forView()
                  .call(CustomersResponseByCity::getCustomers) // <4>
                  .params(List.of("Porto", "London"))
                  .execute().toCompletableFuture().get(1, TimeUnit.SECONDS);

              assertThat(customersResponse.customers()).containsOnly(johanna, bob);
            }
        );
  }
}
// end::view-test[]
