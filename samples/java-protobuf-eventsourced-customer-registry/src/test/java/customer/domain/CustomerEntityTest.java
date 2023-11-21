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

package customer.domain;

import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import kalix.javasdk.testkit.EventSourcedResult;
import com.google.protobuf.Empty;
import customer.api.CustomerApi;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import scala.jdk.javaapi.CollectionConverters;

import static org.junit.jupiter.api.Assertions.*;

public class CustomerEntityTest {

  @Test
  public void exampleTest() {
    CustomerEntityTestKit testKit = CustomerEntityTestKit.of(CustomerEntity::new);
    // use the testkit to execute a command
    // of events emitted, or a final updated state:
    // EventSourcedResult<SomeResponse> result = testKit.someOperation(SomeRequest);
    // verify the emitted events
    // ExpectedEvent actualEvent = result.getNextEventOfType(ExpectedEvent.class);
    // assertEquals(expectedEvent, actualEvent)
    // verify the final state after applying the events
    // assertEquals(expectedState, testKit.getState());
    // verify the response
    // SomeResponse actualResponse = result.getReply();
    // assertEquals(expectedResponse, actualResponse);
  }

  @Test
  public void createTest() {
    CustomerEntityTestKit testKit = CustomerEntityTestKit.of(CustomerEntity::new);
    // EventSourcedResult<Empty> result = testKit.create(Customer.newBuilder()...build());
  }


  @Test
  public void changeNameTest() {
    CustomerEntityTestKit testKit = CustomerEntityTestKit.of(CustomerEntity::new);
    // EventSourcedResult<Empty> result = testKit.changeName(ChangeNameRequest.newBuilder()...build());
  }


  @Test
  public void changeAddressTest() {
    CustomerEntityTestKit testKit = CustomerEntityTestKit.of(CustomerEntity::new);
    // EventSourcedResult<Empty> result = testKit.changeAddress(ChangeAddressRequest.newBuilder()...build());
  }


  @Test
  public void getCustomerTest() {
    CustomerEntityTestKit testKit = CustomerEntityTestKit.of(CustomerEntity::new);
    // EventSourcedResult<Customer> result = testKit.getCustomer(GetCustomerRequest.newBuilder()...build());
  }

}
