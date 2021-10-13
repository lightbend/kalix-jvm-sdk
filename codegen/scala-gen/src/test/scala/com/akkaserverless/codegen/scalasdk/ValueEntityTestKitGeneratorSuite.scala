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

package com.akkaserverless.codegen.scalasdk

import com.akkaserverless.codegen.scalasdk.impl.ValueEntityTestKitGenerator
import com.lightbend.akkasls.codegen.ModelBuilder
import com.lightbend.akkasls.codegen.PackageNaming
import com.lightbend.akkasls.codegen.TestData
import com.lightbend.akkasls.codegen.TestData.fullyQualifiedName

class ValueEntityTestKitGeneratorSuite extends munit.FunSuite {
  private val testData = TestData.scalaStyle
  private val packageNaming = testData.packageNamingTemplate

  test(
    "it can generate an specific TestKit for the proto files " +
    "in test/resources/testkit") {

    val entity = createShoppingCartEntity()
    val service = createShoppingCartService(entity)

    val sourceCode = ValueEntityTestKitGenerator.testkit(entity, service).content

    val expected =
      """|package com.example.shoppingcart.domain
         |
         |import com.akkaserverless.scalasdk.testkit.ValueEntityResult
         |import com.akkaserverless.scalasdk.testkit.impl.TestKitValueEntityContext
         |import com.akkaserverless.scalasdk.testkit.impl.ValueEntityResultImpl
         |import com.akkaserverless.scalasdk.valueentity.ValueEntity
         |import com.akkaserverless.scalasdk.valueentity.ValueEntityContext
         |import com.example.shoppingcart.api
         |import com.google.protobuf.Empty
         |
         |// This code is managed by Akka Serverless tooling.
         |// It will be re-generated to reflect any changes to your protobuf definitions.
         |// DO NOT EDIT
         |
         |/**
         | * TestKit for unit testing ShoppingCart
         | */
         |object ShoppingCartTestKit {
         |  /**
         |   * Create a testkit instance of ShoppingCart
         |   * @param entityFactory A function that creates a ShoppingCart based on the given ValueEntityContext,
         |   *                      a default entity id is used.
         |   */
         |  def apply(entityFactory: ValueEntityContext => ShoppingCart): ShoppingCartTestKit =
         |    apply("testkit-entity-id", entityFactory)
         |
         |  /**
         |   * Create a testkit instance of ShoppingCart with a specific entity id.
         |   */
         |  def apply(entityId: String, entityFactory: ValueEntityContext => ShoppingCart): ShoppingCartTestKit =
         |    new ShoppingCartTestKit(entityFactory(new TestKitValueEntityContext(entityId)))
         |}
         |
         |/**
         | * TestKit for unit testing ShoppingCart
         | */
         |final class ShoppingCartTestKit private(entity: ShoppingCart) {
         |  private var state: Cart = entity.emptyState
         |
         |  /**
         |   * @return The current state of the ShoppingCart under test
         |   */
         |  def currentState(): Cart =
         |    state
         |
         |  private def interpretEffects[Reply](effect: ValueEntity.Effect[Reply]): ValueEntityResult[Reply] = {
         |    val result = new ValueEntityResultImpl[Reply](effect)
         |    if (result.stateWasUpdated)
         |      this.state = result.updatedState.asInstanceOf[Cart]
         |    result
         |  }
         |
         |  def addItem(command: api.AddLineItem): ValueEntityResult[Empty] = {
         |    val effect = entity.addItem(state, command)
         |    interpretEffects(effect)
         |  }
         |
         |  def removeItem(command: api.RemoveLineItem): ValueEntityResult[Empty] = {
         |    val effect = entity.removeItem(state, command)
         |    interpretEffects(effect)
         |  }
         |
         |  def getCart(command: api.GetShoppingCart): ValueEntityResult[api.Cart] = {
         |    val effect = entity.getCart(state, command)
         |    interpretEffects(effect)
         |  }
         |}
         |""".stripMargin

    assertNoDiff(sourceCode, expected)
  }

  test("it can generate an specific Test stub for the entity") {
    val entity = createShoppingCartEntity()
    val service = createShoppingCartService(entity)

    val sourceCode = ValueEntityTestKitGenerator.test(entity, service).content

    val expected =
      """|package com.example.shoppingcart.domain
         |
         |import com.akkaserverless.scalasdk.testkit.ValueEntityResult
         |import com.akkaserverless.scalasdk.valueentity.ValueEntity
         |import com.example.shoppingcart.api
         |import com.google.protobuf.Empty
         |import org.scalatest.matchers.should.Matchers
         |import org.scalatest.wordspec.AnyWordSpec
         |
         |class ShoppingCartSpec
         |    extends AnyWordSpec
         |    with Matchers {
         |
         |  "ShoppingCart" must {
         |
         |    "have example test that can be removed" in {
         |      val testKit = ShoppingCartTestKit(new ShoppingCart(_))
         |      // use the testkit to execute a command
         |      // and verify final updated state:
         |      // val result = testKit.someOperation(SomeRequest)
         |      // verify the response
         |      // val actualResponse = result.getReply()
         |      // actualResponse shouldBe expectedResponse
         |      // verify the final state after the command
         |      // testKit.currentState() shouldBe expectedState
         |    }
         |
         |    "handle command AddItem" in {
         |      val testKit = ShoppingCartTestKit(new ShoppingCart(_))
         |      // val result = testKit.addItem(api.AddLineItem(...))
         |    }
         |
         |    "handle command RemoveItem" in {
         |      val testKit = ShoppingCartTestKit(new ShoppingCart(_))
         |      // val result = testKit.removeItem(api.RemoveLineItem(...))
         |    }
         |
         |    "handle command GetCart" in {
         |      val testKit = ShoppingCartTestKit(new ShoppingCart(_))
         |      // val result = testKit.getCart(api.GetShoppingCart(...))
         |    }
         |
         |  }
         |}
         |""".stripMargin

    assertNoDiff(sourceCode, expected)
  }

  test("it can generate an specific integration test stub for the entity") {
    val entity = createShoppingCartEntity()
    val service = createShoppingCartService(entity)
    val main = fullyQualifiedName("Main", packageNaming.copy(protoPackage = "com.example.shoppingcart"))

    assertEquals(
      ValueEntityTestKitGenerator.integrationTest(main, entity, service).content,
      """package com.example.shoppingcart.api
        |
        |import akka.actor.ActorSystem
        |import com.akkaserverless.scalasdk.testkit.AkkaServerlessTestKit
        |import com.akkaserverless.scalasdk.testkit.ValueEntityResult
        |import com.akkaserverless.scalasdk.valueentity.ValueEntity
        |import com.example.shoppingcart.Main
        |import com.example.shoppingcart.domain
        |import com.google.protobuf.Empty
        |import org.scalatest.BeforeAndAfterAll
        |import org.scalatest.concurrent.ScalaFutures
        |import org.scalatest.matchers.should.Matchers
        |import org.scalatest.time.Millis
        |import org.scalatest.time.Seconds
        |import org.scalatest.time.Span
        |import org.scalatest.wordspec.AnyWordSpec
        |
        |// This class was initially generated based on the .proto definition by Akka Serverless tooling.
        |//
        |// As long as this file exists it will not be overwritten: you can maintain it yourself,
        |// or delete it so it is regenerated as needed.
        |
        |class ShoppingCartServiceIntegrationSpec
        |    extends AnyWordSpec
        |    with Matchers
        |    with BeforeAndAfterAll
        |    with ScalaFutures {
        |
        |  implicit val patience: PatienceConfig =
        |    PatienceConfig(Span(5, Seconds), Span(500, Millis))
        |
        |  val testKit = AkkaServerlessTestKit(Main.createAkkaServerless())
        |  testKit.start()
        |  implicit val system: ActorSystem = testKit.system
        |
        |  "ShoppingCartService" must {
        |    val client: ShoppingCartServiceClient =
        |      ShoppingCartServiceClient(testKit.grpcClientSettings)
        |
        |    "have example test that can be removed" in {
        |      // use the gRPC client to send requests to the
        |      // proxy and verify the results
        |    }
        |
        |  }
        |
        |  override def afterAll() = {
        |    testKit.stop()
        |    super.afterAll()
        |  }
        |}
        |""".stripMargin)
  }

  def createShoppingCartEntity(): ModelBuilder.ValueEntity = {

    val domainProto = {
      packageNaming
        .copy(
          "cart/shoppingcart_domain.proto",
          "ShoppingcartDomain", // Cart here is lowerCase as per protobuf generation
          "com.example.shoppingcart.domain")
    }

    ModelBuilder.ValueEntity(
      domainProto.protoPackage + ".ShoppingCart",
      fullyQualifiedName("ShoppingCart", domainProto),
      "shopping-cart",
      ModelBuilder.State(fullyQualifiedName("Cart", domainProto)))
  }

  /**
   * This ModelBuilder.EntityService is equivalent to service in test/resources/testkit/shoppingcart_api.proto
   */
  def createShoppingCartService(entity: ModelBuilder.Entity): ModelBuilder.EntityService = {
    val shoppingCartProto =
      packageNaming.copy(
        "cart/shoppingcart_api.proto",
        "ShoppingcartApi", // Cart here is lowerCase as per protobuf generation
        "com.example.shoppingcart.api")
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
