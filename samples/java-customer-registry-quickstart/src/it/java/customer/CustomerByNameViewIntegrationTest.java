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
import java.util.concurrent.TimeUnit.*;

import akka.stream.javadsl.Sink;
import io.grpc.StatusRuntimeException;
import com.akkaserverless.javasdk.testkit.junit.AkkaServerlessTestKitResource;
import customer.api.CustomerApi;
import customer.api.CustomerServiceClient;
import customer.domain.CustomerDomain;
import customer.view.CustomerByNameClient;
import customer.view.CustomerViewModel;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static java.util.concurrent.TimeUnit.*;

// Example of an integration test calling our service via the Akka Serverless proxy
// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class CustomerByNameViewIntegrationTest {

  /**
   * The test kit starts both the service container and the Akka Serverless proxy.
   */
  @ClassRule
  public static final AkkaServerlessTestKitResource testkit =
          new AkkaServerlessTestKitResource(Main.createAkkaServerless());

  /**
   * Use the generated gRPC client to call the service through the Akka Serverless proxy.
   */
  private final CustomerServiceClient client;
  private final CustomerByNameClient byNameClient;

  public CustomerByNameViewIntegrationTest() {
    client = CustomerServiceClient.create(testkit.getGrpcClientSettings(), testkit.getActorSystem());
    byNameClient = CustomerByNameClient.create(testkit.getGrpcClientSettings(), testkit.getActorSystem());
  }

  CustomerApi.Customer getCustomer(String customerId) throws Exception {
    return client
        .getCustomer(CustomerApi.GetCustomerRequest.newBuilder().setCustomerId(customerId).build())
        .toCompletableFuture()
        .get(5, SECONDS);
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
          .get(5, SECONDS);
    assertEquals("Johanna", getCustomer(id).getName());

    // FIXME how to nicely wait for the view to be updated?
    Thread.sleep(4000);
    
    List<CustomerDomain.CustomerState> customers = byNameClient.getCustomers(
            CustomerViewModel.ByNameRequest.newBuilder()
                    .setCustomerName("Johanna")
                    .build())
            .runWith(Sink.seq(), testkit.getActorSystem())
                    .toCompletableFuture()
                    .get(5, SECONDS);
    assertEquals(1, customers.size());
    assertEquals("Johanna", customers.get(0).getName());
  }
}
