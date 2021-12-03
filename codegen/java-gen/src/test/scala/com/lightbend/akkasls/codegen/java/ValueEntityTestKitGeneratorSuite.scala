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

package com.lightbend.akkasls.codegen.java

import com.lightbend.akkasls.codegen.ModelBuilder
import com.lightbend.akkasls.codegen.PackageNaming
import com.lightbend.akkasls.codegen.TestData
import com.lightbend.akkasls.codegen.TestData.fullyQualifiedName

class ValueEntityTestKitGeneratorSuite extends munit.FunSuite {
  private val testData = TestData.javaStyle

  test(
    "it can generate an specific TestKit for the proto files " +
    "in test/resources/testkit") {

    val packageName = "com.example.shoppingcart"
    val entity = generateShoppingCartEntity
    val service = generateShoppingCartService(entity)

    val sourceCode = ValueEntityTestKitGenerator.generateSourceCode(service, entity, packageName)

    val expected =
      """package com.example.shoppingcart.domain;
        |
        |import com.akkaserverless.javasdk.impl.effect.MessageReplyImpl;
        |import com.akkaserverless.javasdk.impl.effect.SecondaryEffectImpl;
        |import com.akkaserverless.javasdk.impl.valueentity.ValueEntityEffectImpl;
        |import com.akkaserverless.javasdk.testkit.ValueEntityResult;
        |import com.akkaserverless.javasdk.testkit.impl.TestKitValueEntityContext;
        |import com.akkaserverless.javasdk.testkit.impl.ValueEntityResultImpl;
        |import com.akkaserverless.javasdk.valueentity.ValueEntity;
        |import com.akkaserverless.javasdk.valueentity.ValueEntityContext;
        |import com.example.shoppingcart.domain.ShoppingCartDomain;
        |import com.google.protobuf.Empty;
        |
        |import java.util.function.Function;
        |
        |// This code is managed by Akka Serverless tooling.
        |// It will be re-generated to reflect any changes to your protobuf definitions.
        |// DO NOT EDIT
        |
        |/**
        | * TestKit for unit testing ShoppingCart
        | */
        |public final class ShoppingCartTestKit {
        |
        |  private ShoppingCartDomain.Cart state;
        |  private ShoppingCart entity;
        |
        |  /**
        |   * Create a testkit instance of ShoppingCart
        |   * @param entityFactory A function that creates a ShoppingCart based on the given ValueEntityContext,
        |   *                      a default entity id is used.
        |   */
        |  public static ShoppingCartTestKit of(Function<ValueEntityContext, ShoppingCart> entityFactory) {
        |    return of("testkit-entity-id", entityFactory);
        |  }
        |
        |  /**
        |   * Create a testkit instance of ShoppingCart with a specific entity id.
        |   */
        |  public static ShoppingCartTestKit of(String entityId, Function<ValueEntityContext, ShoppingCart> entityFactory) {
        |    return new ShoppingCartTestKit(entityFactory.apply(new TestKitValueEntityContext(entityId)));
        |  }
        |
        |  /** Construction is done through the static ShoppingCartTestKit.of-methods */
        |  private ShoppingCartTestKit(ShoppingCart entity) {
        |    this.state = entity.emptyState();
        |    this.entity = entity;
        |  }
        |
        |  private ShoppingCartTestKit(ShoppingCart entity, ShoppingCartDomain.Cart state) {
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
        |  private <Reply> ValueEntityResult<Reply> interpretEffects(ValueEntity.Effect<Reply> effect) {
        |    @SuppressWarnings("unchecked")
        |    ValueEntityResultImpl<Reply> result = new ValueEntityResultImpl<>(effect);
        |    if (result.stateWasUpdated()) {
        |      this.state = (ShoppingCartDomain.Cart) result.getUpdatedState();
        |    }
        |    return result;
        |  }
        |
        |  public ValueEntityResult<Empty> addItem(ShoppingCartApi.AddLineItem addLineItem) {
        |    ValueEntity.Effect<Empty> effect = entity.addItem(state, addLineItem);
        |    return interpretEffects(effect);
        |  }
        |
        |  public ValueEntityResult<Empty> removeItem(ShoppingCartApi.RemoveLineItem removeLineItem) {
        |    ValueEntity.Effect<Empty> effect = entity.removeItem(state, removeLineItem);
        |    return interpretEffects(effect);
        |  }
        |
        |  public ValueEntityResult<ShoppingCartApi.Cart> getCart(ShoppingCartApi.GetShoppingCart getShoppingCart) {
        |    ValueEntity.Effect<ShoppingCartApi.Cart> effect = entity.getCart(state, getShoppingCart);
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

    val sourceCode = ValueEntityTestKitGenerator.generateTestSources(service, entity, packageName)

    val expected =
      """package com.example.shoppingcart.domain;
        |
        |import com.akkaserverless.javasdk.testkit.ValueEntityResult;
        |import com.akkaserverless.javasdk.valueentity.ValueEntity;
        |import com.example.shoppingcart.domain.ShoppingCartDomain;
        |import com.google.protobuf.Empty;
        |import org.junit.Test;
        |
        |import static org.junit.Assert.*;
        |
        |// This class was initially generated based on the .proto definition by Akka Serverless tooling.
        |//
        |// As long as this file exists it will not be overwritten: you can maintain it yourself,
        |// or delete it so it is regenerated as needed.
        |
        |public class ShoppingCartTest {
        |
        |  @Test
        |  public void exampleTest() {
        |    ShoppingCartTestKit testKit = ShoppingCartTestKit.of(ShoppingCart::new);
        |    // use the testkit to execute a command
        |    // of events emitted, or a final updated state:
        |    // ValueEntityResult<SomeResponse> result = testKit.someOperation(SomeRequest);
        |    // verify the response
        |    // SomeResponse actualResponse = result.getReply();
        |    // assertEquals(expectedResponse, actualResponse);
        |    // verify the final state after the command
        |    // assertEquals(expectedState, testKit.getState());
        |  }
        |
        |  @Test
        |  public void addItemTest() {
        |    ShoppingCartTestKit testKit = ShoppingCartTestKit.of(ShoppingCart::new);
        |    // ValueEntityResult<Empty> result = testKit.addItem(AddLineItem.newBuilder()...build());
        |  }
        |
        |
        |  @Test
        |  public void removeItemTest() {
        |    ShoppingCartTestKit testKit = ShoppingCartTestKit.of(ShoppingCart::new);
        |    // ValueEntityResult<Empty> result = testKit.removeItem(RemoveLineItem.newBuilder()...build());
        |  }
        |
        |
        |  @Test
        |  public void getCartTest() {
        |    ShoppingCartTestKit testKit = ShoppingCartTestKit.of(ShoppingCart::new);
        |    // ValueEntityResult<Cart> result = testKit.getCart(GetShoppingCart.newBuilder()...build());
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
  def generateShoppingCartEntity(): ModelBuilder.ValueEntity = {

    val domainProto =
      PackageNaming(
        "cart/shoppingcart_domain.proto",
        "ShoppingcartDomain", // Cart here is lowerCase as per protobuf generation
        "com.example.shoppingcart.domain",
        None,
        Some("ShoppingCartDomain"),
        javaMultipleFiles = false)

    ModelBuilder.ValueEntity(
      fullyQualifiedName("ShoppingCart", domainProto),
      "eventsourced-shopping-cart",
      ModelBuilder.State(fullyQualifiedName("Cart", domainProto)))
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
        Some("com.google.protobuf"),
        Some("EmptyProto"),
        javaMultipleFiles = true)
    ModelBuilder.EntityService(
      fullyQualifiedName("ShoppingCartService", shoppingCartProto),
      List(
        testData.command(
          "AddItem",
          fullyQualifiedName("AddLineItem", shoppingCartProto),
          fullyQualifiedName("Empty", googleEmptyProto)),
        testData.command(
          "RemoveItem",
          fullyQualifiedName("RemoveLineItem", shoppingCartProto),
          fullyQualifiedName("Empty", googleEmptyProto)),
        testData.command(
          "GetCart",
          fullyQualifiedName("GetShoppingCart", shoppingCartProto),
          fullyQualifiedName("Cart", shoppingCartProto))),
      entity.fqn.fullName)
  }

}
