package customer.api;

import com.google.protobuf.Empty;
import customer.Main;
import kalix.javasdk.testkit.KalixTestKit;
import kalix.javasdk.testkit.junit.jupiter.KalixTestKitExtension;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.time.Duration.ofSeconds;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

// Example of an integration test calling our service via the Kalix Runtime
// IMPORTANT: this tests depends on an external kafka instance. Make sure to have it running
// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class CustomerActionWithKafkaIntegrationTest {

  /**
   * The test kit starts both the service container and the Kalix Runtime.
   */
  @RegisterExtension
  public static final KalixTestKitExtension testKit;
  static KalixTestKit.Settings settings;

  static {
    settings = KalixTestKit.Settings.DEFAULT.withEventingSupport(KalixTestKit.Settings.EventingSupport.KAFKA);
    testKit = new KalixTestKitExtension(Main.createKalix(),
        settings);
  }


  /**
   * Use the generated gRPC client to call the service through the Kalix Runtime.
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
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "0.0.0.0:9093");
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "it-test-" + System.currentTimeMillis()); // using new consumer group every run
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    // Create Kafka consumer and subscribe to topic
    KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(props);
    consumer.subscribe(Collections.singletonList("customer_changes"));

    await()
        .ignoreExceptions()
        .atMost(ofSeconds(30))
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
