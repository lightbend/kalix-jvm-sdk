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

package com.lightbend.akkasls.codegen
package java

class EventSourcedEntityTestKitGeneratorSuite extends munit.FunSuite {
  private val testData = TestData()

  test(
    "it can generate an specific TestKit for the proto files " +
    "in test/resources/testkit") {

    val packageName = "com.example.shoppingcart"
    val entity = generateShoppingCartEntity
    val service = generateShoppingCartService(entity)
    val className = "ShoppingCart"

    val sourceCode = EventSourcedEntityTestKitGenerator.generateSourceCode(service, entity, packageName, className)

    val expected =
      """/* This code is managed by Akka Serverless tooling.
        | * It will be re-generated to reflect any changes to your protobuf definitions.
        | * DO NOT EDIT
        | */
        |package com.example.shoppingcart.domain;
        |
        |import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity;
        |import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityContext;
        |import com.akkaserverless.javasdk.impl.effect.MessageReplyImpl;
        |import com.akkaserverless.javasdk.impl.effect.SecondaryEffectImpl;
        |import com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl;
        |import com.akkaserverless.javasdk.testkit.EventSourcedResult;
        |import com.akkaserverless.javasdk.testkit.impl.EventSourcedResultImpl;
        |import com.akkaserverless.javasdk.testkit.impl.TestKitEventSourcedEntityContext;
        |import com.example.shoppingcart.domain.ShoppingCartDomain;
        |import com.google.protobuf.Empty;
        |import java.util.ArrayList;
        |import java.util.List;
        |import java.util.NoSuchElementException;
        |import java.util.function.Function;
        |import scala.jdk.javaapi.CollectionConverters;
        |
        |/**
        | * TestKit for unit testing ShoppingCart
        | */
        |public final class ShoppingCartTestKit {
        |
        |  private ShoppingCartDomain.Cart state;
        |  private ShoppingCart entity;
        |  private List<Object> events = new ArrayList<Object>();
        |
        |  /**
        |   * Create a testkit instance of ShoppingCart
        |   * @param entityFactory A function that creates a ShoppingCart based on the given EventSourcedEntityContext,
        |   *                      a default entity id is used.
        |   */
        |  public static ShoppingCartTestKit of(Function<EventSourcedEntityContext, ShoppingCart> entityFactory) {
        |    return of("testkit-entity-id", entityFactory);
        |  }
        |
        |  /**
        |   * Create a testkit instance of ShoppingCart with a specific entity id.
        |   */
        |  public static ShoppingCartTestKit of(String entityId, Function<EventSourcedEntityContext, ShoppingCart> entityFactory) {
        |    return new ShoppingCartTestKit(entityFactory.apply(new TestKitEventSourcedEntityContext(entityId)));
        |  }
        |
        |  /** Construction is done through the static ShoppingCartTestKit.of-methods */
        |  private ShoppingCartTestKit(ShoppingCart entity) {
        |    this.state = entity.emptyState();
        |    this.entity = entity;
        |  }
        |
        |  public ShoppingCartTestKit(ShoppingCart entity, ShoppingCartDomain.Cart state) {
        |    this.state = state;
        |    this.entity = entity;
        |  }
        |
        |  /**
        |   * @return The current state of the ShoppingCart under test
        |   */
        |  public ShoppingCartDomain.Cart getState() {
        |    return state;
        |  }
        |
        |  /**
        |   * @return All events that has been emitted by command handlers since the creation of this testkit.
        |   *         Individual sets of events from a single command handler invokation can be found in the
        |   *         Result from calling it.
        |   */
        |  public List<Object> getAllEvents() {
        |    return this.events;
        |  }
        |
        |  private ShoppingCartDomain.Cart handleEvent(ShoppingCartDomain.Cart state, Object event) {
        |    if (event instanceof ShoppingCartDomain.ItemAdded) {
        |      return entity.itemAdded(state, (ShoppingCartDomain.ItemAdded) event);
        |    } else if (event instanceof ShoppingCartDomain.ItemRemoved) {
        |      return entity.itemRemoved(state, (ShoppingCartDomain.ItemRemoved) event);
        |    } else {
        |      throw new NoSuchElementException("Unknown event type [" + event.getClass() + "]");
        |    }
        |  }
        |
        |  @SuppressWarnings("unchecked")
        |  private <Reply> EventSourcedResult<Reply> interpretEffects(EventSourcedEntity.Effect<Reply> effect) {
        |    List<Object> events = EventSourcedResultImpl.eventsOf(effect);
        |    this.events.addAll(events);
        |    for(Object e: events) {
        |      this.state = handleEvent(state,e);
        |    }
        |    return new EventSourcedResultImpl(effect, state);
        |  }
        |
        |  public EventSourcedResult<Empty> addItem(ShoppingCartApi.AddLineItem command) {
        |    EventSourcedEntity.Effect<Empty> effect = entity.addItem(state, command);
        |    return interpretEffects(effect);
        |  }
        |
        |  public EventSourcedResult<Empty> removeItem(ShoppingCartApi.RemoveLineItem command) {
        |    EventSourcedEntity.Effect<Empty> effect = entity.removeItem(state, command);
        |    return interpretEffects(effect);
        |  }
        |
        |  public EventSourcedResult<ShoppingCartApi.Cart> getCart(ShoppingCartApi.GetShoppingCart command) {
        |    EventSourcedEntity.Effect<ShoppingCartApi.Cart> effect = entity.getCart(state, command);
        |    return interpretEffects(effect);
        |  }
        |}
        |""".stripMargin

    assertNoDiff(sourceCode, expected)
  }

  test("it can generate an specific Test stub for the entity") {

    val packageName = "com.example.shoppingcart"
    val entity = generateShoppingCartEntity
    val service = generateShoppingCartService(entity)

    val sourceCode = EventSourcedEntityTestKitGenerator.generateTestSources(service, entity, packageName)

    val expected =
      """/* This code was generated by Akka Serverless tooling.
        | * As long as this file exists it will not be re-generated.
        | * You are free to make changes to this file.
        | */
        |package com.example.shoppingcart.domain;
        |
        |import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity;
        |import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityContext;
        |import com.akkaserverless.javasdk.testkit.EventSourcedResult;
        |import com.example.shoppingcart.domain.ShoppingCartDomain;
        |import com.google.protobuf.Empty;
        |import java.util.ArrayList;
        |import java.util.List;
        |import java.util.NoSuchElementException;
        |import org.junit.Test;
        |import scala.jdk.javaapi.CollectionConverters;
        |
        |import static org.junit.Assert.*;
        |
        |public class ShoppingCartTest {
        |
        |  @Test
        |  public void exampleTest() {
        |    ShoppingCartTestKit testKit = ShoppingCartTestKit.of(ShoppingCart::new);
        |    // use the testkit to execute a command
        |    // of events emitted, or a final updated state:
        |    // EventSourcedResult<SomeResponse> result = testKit.someOperation(SomeRequest);
        |    // verify the emitted events
        |    // ExpectedEvent actualEvent = result.getNextEventOfType(ExpectedEvent.class);
        |    // assertEquals(expectedEvent, actualEvent)
        |    // verify the final state after applying the events
        |    // assertEquals(expectedState, testKit.getState());
        |    // verify the response
        |    // SomeResponse actualResponse = result.getReply();
        |    // assertEquals(expectedResponse, actualResponse);
        |  }
        |
        |  @Test
        |  public void addItemTest() {
        |    ShoppingCartTestKit testKit = ShoppingCartTestKit.of(ShoppingCart::new);
        |    // EventSourcedResult<Empty> result = testKit.addItem(AddLineItem.newBuilder()...build());
        |  }
        |
        |
        |  @Test
        |  public void removeItemTest() {
        |    ShoppingCartTestKit testKit = ShoppingCartTestKit.of(ShoppingCart::new);
        |    // EventSourcedResult<Empty> result = testKit.removeItem(RemoveLineItem.newBuilder()...build());
        |  }
        |
        |
        |  @Test
        |  public void getCartTest() {
        |    ShoppingCartTestKit testKit = ShoppingCartTestKit.of(ShoppingCart::new);
        |    // EventSourcedResult<Cart> result = testKit.getCart(GetShoppingCart.newBuilder()...build());
        |  }
        |
        |}
        |""".stripMargin

    assertNoDiff(sourceCode, expected)
  }

  /**
   * This ModelBuilder.EventSourcedEntity is equivalent to the entity in
   * test/resources/testkit/shoppingcart_domain.proto
   */
  def generateShoppingCartEntity(): ModelBuilder.EventSourcedEntity = {

    val domainProto =
      PackageNaming(
        "cart/shoppingcart_domain.proto",
        "ShoppingcartDomain", // Cart here is lowerCase as per protobuf generation
        "com.example.shoppingcart.domain",
        None,
        Some("ShoppingCartDomain"),
        javaMultipleFiles = false)

    ModelBuilder.EventSourcedEntity(
      FullyQualifiedName("ShoppingCart", domainProto),
      "eventsourced-shopping-cart",
      ModelBuilder.State(FullyQualifiedName("Cart", domainProto)),
      List(
        ModelBuilder.Event(FullyQualifiedName("ItemAdded", domainProto)),
        ModelBuilder.Event(FullyQualifiedName("ItemRemoved", domainProto))))
  }

  /**
   * This ModelBuilder.EntityService is equivalent to service in test/resources/testkit/shoppingcart_api.proto
   */
  def generateShoppingCartService(entity: ModelBuilder.Entity): ModelBuilder.EntityService = {
    val shoppingCartProto =
      PackageNaming(
        "cart/shoppingcart_api.proto",
        "ShoppingcartApi", // Cart here is lowerCase as per protobuf generation
        "com.example.shoppingcart",
        None,
        Some("ShoppingCartApi"),
        javaMultipleFiles = false)
    val googleEmptyProto =
      PackageNaming(
        "Empty",
        "Empty",
        "google.protobuf",
        Some(
          "com.google.protobuf" +
          "" +
          ""),
        Some("EmptyProto"),
        javaMultipleFiles = true)
    ModelBuilder.EntityService(
      FullyQualifiedName("ShoppingCartService", shoppingCartProto),
      FullyQualifiedName("ShoppingCartServiceProto", shoppingCartProto),
      List(
        testData.command(
          "AddItem",
          FullyQualifiedName("AddLineItem", shoppingCartProto),
          FullyQualifiedName("Empty", googleEmptyProto)),
        testData.command(
          "RemoveItem",
          FullyQualifiedName("RemoveLineItem", shoppingCartProto),
          FullyQualifiedName("Empty", googleEmptyProto)),
        testData.command(
          "GetCart",
          FullyQualifiedName("GetShoppingCart", shoppingCartProto),
          FullyQualifiedName("Cart", shoppingCartProto))),
      entity.fqn.fullName)
  }

}
