package customer.api;

import com.google.protobuf.Empty;
import customer.Main;
import kalix.javasdk.testkit.KalixTestKit;
import kalix.javasdk.testkit.junit.KalixTestKitResource;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.ClassRule;
import org.junit.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

// Example of an integration test calling our service via the Kalix proxy
// IMPORTANT: this tests depends on an external kafka instance. Make sure to have it running
// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class CustomerActionWithKafkaIntegrationTest {

  /**
   * The test kit starts both the service container and the Kalix proxy.
   */
  @ClassRule
  public static final KalixTestKitResource testKit =
    new KalixTestKitResource(Main.createKalix(),
        KalixTestKit.Settings.DEFAULT.withEventingSupport(KalixTestKit.Settings.EventingSupport.KAFKA));

  /**
   * Use the generated gRPC client to call the service through the Kalix proxy.
   */
  private final CustomerService client;


  public CustomerActionWithKafkaIntegrationTest() {
    client = testKit.getGrpcClient(CustomerService.class);
  }

  @Test
  public void createAndPublish() throws Exception {
    var id = UUID.randomUUID().toString();
    var customer = buildCustomer(id, "Johanna", "foo@example.com", "Porto", "Long Road");
    createCustomer(customer);

    //change customer name
    var completeName = "Johanna Doe";
    client.changeName(CustomerApi.ChangeNameRequest.newBuilder()
            .setCustomerId(id)
            .setNewName(completeName)
            .build())
        .toCompletableFuture()
        .get(5, SECONDS);


    // Example Kafka consumer configuration
    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9093");
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "it-test-" + System.currentTimeMillis()); // using new consumer group every run
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    // Create Kafka consumer and subcribe to topic
    KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(props);
    consumer.subscribe(Collections.singletonList("customer_changes"));

    await()
        .ignoreExceptions()
        .atMost(20, TimeUnit.of(ChronoUnit.SECONDS))
        .untilAsserted(() -> {
          ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(200));
          var foundRecord = false;
          for (ConsumerRecord<String, byte[]> r : records) {
            var customerState = CustomerApi.Customer.parseFrom(r.value());
            if (completeName.equals(customerState.getName())) {
              foundRecord = true;
              break;
            }
          }

          assertTrue(foundRecord);
        });
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

}
