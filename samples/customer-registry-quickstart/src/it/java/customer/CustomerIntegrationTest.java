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

  void changeName(String customerId, String newName) throws Exception {
    client
        .changeName(
            CustomerApi.ChangeNameRequest.newBuilder()
                .setCustomerId(customerId)
                .setNewName(newName)
                .build())
        .toCompletableFuture()
        .get();
  }

  void changeAddress(String customerId, String street, String city) throws Exception {
    client
        .changeAddress(
            CustomerApi.ChangeAddressRequest.newBuilder()
                .setCustomerId(customerId)
                .setNewAddress(
                        CustomerApi.Address.newBuilder()
                                .setStreet(street)
                                .setCity(city)
                                .build()
                )
                .build())
        .toCompletableFuture()
        .get();
  }

  @Test
  public void emptyCustomerByDefault() throws Exception {
    assertEquals("shopping cart should be empty", "xxx", getCustomer("user1").getName());
  }

//  @Test
//  public void addItemsToCart() throws Exception {
//    addItem("cart2", "a", "Apple", 1);
//    addItem("cart2", "b", "Banana", 2);
//    addItem("cart2", "c", "Cantaloupe", 3);
//    CustomerApi.Cart cart = getCart("cart2");
//    assertEquals("shopping cart should have 3 items", 3, cart.getItemsCount());
//    assertEquals(
//        "shopping cart should have expected items",
//        cart.getItemsList(),
//        List.of(item("a", "Apple", 1), item("b", "Banana", 2), item("c", "Cantaloupe", 3)));
//  }
//
//  @Test
//  public void removeItemsFromCart() throws Exception {
//    addItem("cart3", "a", "Apple", 1);
//    addItem("cart3", "b", "Banana", 2);
//    CustomerApi.Cart cart1 = getCart("cart3");
//    assertEquals("shopping cart should have 2 items", 2, cart1.getItemsCount());
//    assertEquals(
//        "shopping cart should have expected items",
//        cart1.getItemsList(),
//        List.of(item("a", "Apple", 1), item("b", "Banana", 2)));
//    removeItem("cart3", "a");
//    CustomerApi.Cart cart2 = getCart("cart3");
//    assertEquals("shopping cart should have 1 item", 1, cart2.getItemsCount());
//    assertEquals(
//        "shopping cart should have expected items",
//        cart2.getItemsList(),
//        List.of(item("b", "Banana", 2)));
//  }
//
//  @Test
//  public void removeCart() throws Exception {
//    addItem("cart4", "a", "Apple", 42);
//    CustomerApi.Cart cart1 = getCart("cart4");
//    assertEquals("shopping cart should have 1 item", 1, cart1.getItemsCount());
//    assertEquals(
//        "shopping cart should have expected items",
//        cart1.getItemsList(),
//        List.of(item("a", "Apple", 42)));
//    removeCart("cart4");
//    assertEquals("shopping cart should be empty", 0, getCart("cart4").getItemsCount());
//  }
}