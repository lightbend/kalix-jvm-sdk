/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package customer;

import java.util.concurrent.ExecutionException;

import io.grpc.StatusRuntimeException;
import com.akkaserverless.javasdk.testkit.junit.AkkaServerlessTestkitResource;
import customer.api.CustomerApi;
import customer.api.CustomerServiceClient;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static java.util.concurrent.TimeUnit.*;

// Example of an integration test calling our service via the Akka Serverless proxy
// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class CustomerIntegrationTest {

  /**
   * The test kit starts both the service container and the Akka Serverless proxy.
   */
  @ClassRule
  public static final AkkaServerlessTestkitResource testkit = new AkkaServerlessTestkitResource(Main.SERVICE);

  /**
   * Use the generated gRPC client to call the service through the Akka Serverless proxy.
   */
  private final CustomerServiceClient client;

  public CustomerIntegrationTest() {
    client = CustomerServiceClient.create(testkit.getGrpcClientSettings(), testkit.getActorSystem());
  }

  CustomerApi.Customer getCustomer(String customerId) throws Exception {
    return client
        .getCustomer(CustomerApi.GetCustomerRequest.newBuilder().setCustomerId(customerId).build())
        .toCompletableFuture()
        .get();
  }

  @Test(expected = StatusRuntimeException.class)
  public void errorGettingNonExistingCustomer() throws Throwable {
    try {
      getCustomer("user1");
    } catch (ExecutionException e) {
      throw e.getCause();
    }
  }

  @Test
  public void createCustomer() throws Exception {
    String id = "42";
    client.create(CustomerApi.Customer.newBuilder()
            .setCustomerId(id)
            .setName("Johanna")
            .setEmail("foo@example.com")
            .build())
          .toCompletableFuture()
          .get();
    assertEquals("Johanna", getCustomer(id).getName());
  }

  @Test
  public void changeName() throws Exception {
    String id = "43";
    client.create(CustomerApi.Customer.newBuilder()
                    .setCustomerId(id)
                    .setName("Johanna")
                    .setEmail("foo@example.com")
                    .build())
            .toCompletableFuture()
            .get();
    client.changeName(CustomerApi.ChangeNameRequest.newBuilder()
            .setCustomerId(id)
            .setNewName("Katarina")
            .build())
        .toCompletableFuture()
        .get();
    assertEquals("Katarina", getCustomer(id).getName());
  }

  @Test
  public void changeAddress() throws Exception {
    String id = "44";
    client.create(CustomerApi.Customer.newBuilder()
                    .setCustomerId(id)
                    .setName("Johanna")
                    .setEmail("foo@example.com")
                    .build())
            .toCompletableFuture()
            .get();
    client.changeAddress(CustomerApi.ChangeAddressRequest.newBuilder()
                    .setCustomerId(id)
                    .setNewAddress(
                            CustomerApi.Address.newBuilder()
                                    .setStreet("Elm st. 5")
                                    .setCity("New Orleans")
                                    .build()
                    )
                    .build())
            .toCompletableFuture()
            .get();
    assertEquals("Elm st. 5", getCustomer(id).getAddress().getStreet());
  }
}
