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

import com.akkaserverless.codegen.scalasdk.impl.EventSourcedEntityTestKitGenerator
import com.lightbend.akkasls.codegen.{ FullyQualifiedName, ModelBuilder, PackageNaming, TestData }

class EventSourcedEntityTestKitGeneratorSuite extends munit.FunSuite {
  private val testData = TestData.scalaStyle
  private val packageNaming = testData.packageNamingTemplate

  test("it can generate an specific TestKit for the proto files") {
    assertEquals(
      EventSourcedEntityTestKitGenerator.testKit(testData.eventSourcedEntity(), testData.simpleEntityService()).content,
      s"""package com.example.service.domain
         |
         |import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity
         |import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityContext
         |import com.akkaserverless.scalasdk.testkit.EventSourcedResult
         |import com.akkaserverless.scalasdk.testkit.impl.EventSourcedResultImpl
         |import com.akkaserverless.scalasdk.testkit.impl.TestKitEventSourcedEntityContext
         |import com.example.service
         |import com.external.Empty
         |
         |// This code is managed by Akka Serverless tooling.
         |// It will be re-generated to reflect any changes to your protobuf definitions.
         |// DO NOT EDIT
         |
         |/**
         | * TestKit for unit testing MyEntity
         | */
         |object MyEntityTestKit {
         |  /**
         |   * Create a testkit instance of MyEntity
         |   * @param entityFactory A function that creates a MyEntity based on the given EventSourcedEntityContext,
         |   *                      a default entity id is used.
         |   */
         |  def apply(entityFactory: EventSourcedEntityContext => MyEntity): MyEntityTestKit =
         |    apply("testkit-entity-id", entityFactory)
         |  /**
         |   * Create a testkit instance of MyEntity with a specific entity id.
         |   */
         |  def apply(entityId: String, entityFactory: EventSourcedEntityContext => MyEntity): MyEntityTestKit =
         |    new MyEntityTestKit(entityFactory(new TestKitEventSourcedEntityContext(entityId)))
         |}
         |final class MyEntityTestKit private(entity: MyEntity) {
         |  private var state: MyState = entity.emptyState
         |  private var events: Seq[Any] = Nil
         |
         |  private def handleEvent(state: MyState, event: Any): MyState =
         |   event match {
         |     case e: SetEvent =>
         |      entity.setEvent(state, e)
         |   }
         |
         |  private def interpretEffects[R](effect: EventSourcedEntity.Effect[R]): EventSourcedResult[R] = {
         |    val events = EventSourcedResultImpl.eventsOf(effect)
         |    this.events ++= events
         |    this.state = events.foldLeft(this.state)(handleEvent)
         |    new EventSourcedResultImpl[R, MyState](effect, state)
         |  }
         |
         |  def set(command: service.SetValue): EventSourcedResult[Empty] =
         |    interpretEffects(entity.set(state, command))
         |
         |  def get(command: service.GetValue): EventSourcedResult[service.MyState] =
         |    interpretEffects(entity.get(state, command))
         |}
         |""".stripMargin)
  }

  test("it can generate an specific Test stub for the entity") {
    assertEquals(
      EventSourcedEntityTestKitGenerator.test(testData.eventSourcedEntity(), testData.simpleEntityService()).content,
      s"""package com.example.service.domain
         |
         |import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity
         |import com.akkaserverless.scalasdk.testkit.EventSourcedResult
         |import com.example.service
         |import com.external.Empty
         |import org.scalatest.matchers.should.Matchers
         |import org.scalatest.wordspec.AnyWordSpec
         |
         |// This class was initially generated based on the .proto definition by Akka Serverless tooling.
         |//
         |// As long as this file exists it will not be overwritten: you can maintain it yourself,
         |// or delete it so it is regenerated as needed.
         |
         |class MyEntitySpec extends AnyWordSpec with Matchers {
         |  "The MyEntity" should {
         |    "have example test that can be removed" in {
         |      val testKit = MyEntityTestKit(new MyEntity(_))
         |      // use the testkit to execute a command:
         |      // val result: EventSourcedResult[R] = testKit.someOperation(SomeRequest("id"));
         |      // verify the emitted events
         |      // val actualEvent: ExpectedEvent = result.nextEventOfType[ExpectedEvent]
         |      // actualEvent shouldBe expectedEvent
         |      // verify the final state after applying the events
         |      // testKit.state() shouldBe expectedState
         |      // verify the response
         |      // result.reply shouldBe expectedReply
         |      // verify the final state after the command
         |    }
         |
         |    "correctly process commands of type Set" in {
         |      val testKit = MyEntityTestKit(new MyEntity(_))
         |      // val result: EventSourcedResult[Empty] = testKit.set(service.SetValue(...))
         |    }
         |
         |    "correctly process commands of type Get" in {
         |      val testKit = MyEntityTestKit(new MyEntity(_))
         |      // val result: EventSourcedResult[service.MyState] = testKit.get(service.GetValue(...))
         |    }
         |  }
         |}
         |""".stripMargin)
  }

  test("it can generate an specific integration test stub for the entity") {
    val main = FullyQualifiedName("Main", packageNaming.copy(protoPackage = "com.example"))

    assertEquals(
      EventSourcedEntityTestKitGenerator
        .integrationTest(main, testData.eventSourcedEntity(), testData.simpleEntityService())
        .content,
      s"""package com.example.service
         |
         |import akka.actor.ActorSystem
         |import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity
         |import com.akkaserverless.scalasdk.testkit.AkkaServerlessTestKit
         |import com.akkaserverless.scalasdk.testkit.EventSourcedResult
         |import com.example.Main
         |import com.external.Empty
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
         |class MyServiceIntegrationSpec
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
         |  "MyService" must {
         |    val client: MyServiceClient =
         |      MyServiceClient(testKit.grpcClientSettings)
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

}
