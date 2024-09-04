package customer.api;


import com.google.protobuf.any.Any;
import customer.Main;
import customer.api.CustomerEntity.Confirm;
import customer.domain.Address;
import customer.domain.Customer;
import customer.view.CustomerView;
import kalix.javasdk.DeferredCall;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.awaitility.Awaitility.await;


@SpringBootTest(classes = Main.class)
public class CustomerIntegrationTest extends KalixIntegrationTestKitSupport {

  @Autowired
  private WebClient webClient;

  private Duration timeout = Duration.of(5, SECONDS);

  @Test
  public void create() {
    String id = UUID.randomUUID().toString();
    Customer customer = new Customer("foo@example.com", "Johanna", null);

    Confirm response = execute(componentClient.forEventSourcedEntity(id)
      .call(CustomerEntity::create)
      .params(customer));

    Assertions.assertEquals(Confirm.done, response);
    Assertions.assertEquals("Johanna", getCustomerById(id).name());
  }

  @Test
  public void changeName() {
    String id = UUID.randomUUID().toString();
    Customer customer = new Customer("foo@example.com", "Johanna", null);

    Confirm response = execute(componentClient.forEventSourcedEntity(id)
      .call(CustomerEntity::create)
      .params(customer));

    Assertions.assertEquals(Confirm.done, response);

    Confirm resUpdate = execute(componentClient.forEventSourcedEntity(id)
      .call(CustomerEntity::changeName)
      .params("Katarina"));


    Assertions.assertEquals(Confirm.done, resUpdate);
    Assertions.assertEquals("Katarina", getCustomerById(id).name());
  }

  @Test
  public void changeAddress() {
    String id = UUID.randomUUID().toString();
    Customer customer = new Customer("foo@example.com", "Johanna", null);

    Confirm response = execute(componentClient.forEventSourcedEntity(id)
      .call(CustomerEntity::create)
      .params(customer));

    Assertions.assertEquals(Confirm.done, response);

    Address address = new Address("Elm st. 5", "New Orleans");

    Confirm resUpdate = execute(componentClient.forEventSourcedEntity(id)
      .call(CustomerEntity::changeAddress)
      .params(address));

    Assertions.assertEquals(Confirm.done, resUpdate);
    Assertions.assertEquals("Elm st. 5", getCustomerById(id).address().street());
  }


  @Test
  public void findByName() {
    String id = UUID.randomUUID().toString();
    Customer customer = new Customer("foo@example.com", "Foo", null);
    Confirm response = execute(componentClient.forEventSourcedEntity(id)
      .call(CustomerEntity::create)
      .params(customer));

    Assertions.assertEquals(Confirm.done, response);

    // the view is eventually updated
    await()
      .ignoreExceptions()
      .atMost(20, TimeUnit.SECONDS)
      .until(() ->
          webClient.get()
            .uri("/customer/by_name/Foo")
                    .retrieve()
                    .bodyToMono(CustomerView.class)
                    .block(timeout)
                    .name(),
            new IsEqual("Foo")
        );
  }

  @Test
  public void findByEmail() {
    String id = UUID.randomUUID().toString();
    Customer customer = new Customer("bar@example.com", "Bar", null);
    Confirm response = execute(componentClient.forEventSourcedEntity(id)
      .call(CustomerEntity::create)
      .params(customer));

    Assertions.assertEquals(Confirm.done, response);

    // the view is eventually updated
    await()
      .ignoreExceptions()
      .atMost(20, TimeUnit.SECONDS)
      .until(() ->
          webClient.get()
            .uri("/customer/by_email/bar@example.com")
            .retrieve()
            .bodyToMono(CustomerView.class)
            .block(timeout)
            .name(),
        new IsEqual("Bar")
      );
  }

  private Customer getCustomerById(String customerId) {
    return execute(componentClient.forEventSourcedEntity(customerId)
      .call(CustomerEntity::getCustomer));
  }

  protected <T> T execute(DeferredCall<Any, T> deferredCall) {
    try {
      return deferredCall.execute().toCompletableFuture().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

}
