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

package com.akkaserverless.codegen.scalasdk.impl

import com.akkaserverless.codegen.scalasdk.File
import com.lightbend.akkasls.codegen.{ Format, FullyQualifiedName, ModelBuilder }

object EventSourcedEntityTestKitGenerator {
  import com.lightbend.akkasls.codegen.SourceGeneratorUtils._

  def generateUnmanagedTest(
      main: FullyQualifiedName,
      entity: ModelBuilder.EventSourcedEntity,
      service: ModelBuilder.EntityService): Seq[File] =
    Seq(test(entity, service), integrationTest(main, entity, service))

  def generateManagedTest(
      eventSourcedEntity: ModelBuilder.EventSourcedEntity,
      service: ModelBuilder.EntityService): Seq[File] =
    Seq(testKit(eventSourcedEntity, service))

  def testKit(entity: ModelBuilder.EventSourcedEntity, service: ModelBuilder.EntityService): File = {
    val className = s"${entity.fqn.name}TestKit"

    implicit val imports =
      generateImports(
        Seq(entity.state.fqn) ++
        service.commands.map(_.inputType) ++
        service.commands.map(_.outputType),
        entity.fqn.parent.scalaPackage,
        Seq(
          "com.akkaserverless.scalasdk.testkit.EventSourcedResult",
          "com.akkaserverless.scalasdk.testkit.impl.EventSourcedResultImpl",
          "com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity",
          "com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityContext",
          "com.akkaserverless.scalasdk.testkit.impl.TestKitEventSourcedEntityContext"),
        packageImports = Seq(service.fqn.parent.scalaPackage),
        semi = false)

    val eventHandlers = entity.events.map { event =>
      s"""|case e: ${typeName(event.fqn)} =>
          |  entity.${lowerFirst(event.fqn.name)}(state, e)
         |""".stripMargin
    }

    val methods = service.commands.map { cmd =>
      s"""|def ${lowerFirst(cmd.name)}(command: ${typeName(cmd.inputType)}): EventSourcedResult[${typeName(
        cmd.outputType)}] =
          |  interpretEffects(entity.${lowerFirst(cmd.name)}(state, command))
         |""".stripMargin
    }

    File(
      entity.fqn.fileBasename + "TestKit.scala",
      s"""package ${entity.fqn.parent.scalaPackage}
       |
       |$imports
       |
       |$managedComment
       |
       |/**
       | * TestKit for unit testing ${entity.fqn.name}
       | */
       |object $className {
       |  /**
       |   * Create a testkit instance of ${entity.fqn.name}
       |   * @param entityFactory A function that creates a ${entity.fqn.name} based on the given EventSourcedEntityContext,
       |   *                      a default entity id is used.
       |   */
       |  def apply(entityFactory: EventSourcedEntityContext => ${typeName(entity.fqn)}): $className =
       |    apply("testkit-entity-id", entityFactory)
       |  /**
       |   * Create a testkit instance of ${entity.fqn.name} with a specific entity id.
       |   */
       |  def apply(entityId: String, entityFactory: EventSourcedEntityContext => ${entity.fqn.name}): ${{
        entity.fqn.name
      }}TestKit =
       |    new ${entity.fqn.name}TestKit(entityFactory(new TestKitEventSourcedEntityContext(entityId)))
       |}
       |final class $className private(entity: ${typeName(entity.fqn)}) {
       |  private var state: ${typeName(entity.state.fqn)} = entity.emptyState
       |  private var events: Seq[Any] = Nil
       |
       |  private def handleEvent(state: ${typeName(entity.state.fqn)}, event: Any): ${typeName(entity.state.fqn)} =
       |   event match {
       |     ${Format.indent(eventHandlers, 4)}
       |   }
       |
       |  private def interpretEffects[R](effect: EventSourcedEntity.Effect[R]): EventSourcedResult[R] = {
       |    val events = EventSourcedResultImpl.eventsOf(effect)
       |    this.events ++= events
       |    this.state = events.foldLeft(this.state)(handleEvent)
       |    new EventSourcedResultImpl[R, ${typeName(entity.state.fqn)}](effect, state)
       |  }
       |
       |  ${Format.indent(methods, 2)}
       |}
       |""".stripMargin)
  }

  def test(entity: ModelBuilder.EventSourcedEntity, service: ModelBuilder.EntityService): File = {
    val className = s"${entity.fqn.name}Spec"
    implicit val imports = generateImports(
      Seq(entity.state.fqn) ++
      service.commands.map(_.inputType) ++
      service.commands.map(_.outputType),
      entity.fqn.parent.scalaPackage,
      Seq(
        "com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity",
        "com.akkaserverless.scalasdk.testkit.EventSourcedResult",
        "org.scalatest.matchers.should.Matchers",
        "org.scalatest.wordspec.AnyWordSpec"),
      packageImports = Seq(service.fqn.parent.scalaPackage),
      semi = false)

    val testKitClassName = s"${entity.fqn.name}TestKit"

    val dummyTestCases = service.commands.map { command =>
      s"""|"correctly process commands of type ${command.name}" in {
          |  val testKit = $testKitClassName(new ${entity.fqn.name}(_))
          |  // val result: EventSourcedResult[${typeName(command.outputType)}] = testKit.${lowerFirst(
        command.name)}(${typeName(command.inputType)}(...))
          |}
         |""".stripMargin
    }

    File(
      entity.fqn.fileBasename + "Spec.scala",
      s"""package ${entity.fqn.parent.scalaPackage}
         |
         |$imports
         |
         |$unmanagedComment
         |
         |class $className extends AnyWordSpec with Matchers {
         |  "The ${entity.fqn.name}" should {
         |    "have example test that can be removed" in {
         |      val testKit = $testKitClassName(new ${entity.fqn.name}(_))
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
         |    ${Format.indent(dummyTestCases, 4)}
         |  }
         |}
         |""".stripMargin)
  }

  def integrationTest(
      main: FullyQualifiedName,
      valueEntity: ModelBuilder.EventSourcedEntity,
      service: ModelBuilder.EntityService): File = {

    val client = FullyQualifiedName(service.fqn.name + "Client", service.fqn.parent)

    implicit val imports: Imports =
      generateImports(
        Seq(main, valueEntity.state.fqn, client) ++
        service.commands.map(_.inputType) ++
        service.commands.map(_.outputType),
        service.fqn.parent.scalaPackage,
        otherImports = Seq(
          "akka.actor.ActorSystem",
          "com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity",
          "com.akkaserverless.scalasdk.testkit.EventSourcedResult",
          "com.akkaserverless.scalasdk.testkit.AkkaServerlessTestKit",
          "org.scalatest.matchers.should.Matchers",
          "org.scalatest.wordspec.AnyWordSpec",
          "org.scalatest.BeforeAndAfterAll",
          "org.scalatest.concurrent.ScalaFutures",
          "org.scalatest.time.Span",
          "org.scalatest.time.Seconds",
          "org.scalatest.time.Millis"),
        packageImports = Seq(valueEntity.fqn.parent.scalaPackage),
        semi = false)

    val entityClassName = service.fqn.name

    File(
      service.fqn.fileBasename + "IntegrationSpec.scala",
      s"""|package ${service.fqn.parent.scalaPackage}
          |
          |$imports
          |
          |$unmanagedComment
          |
          |class ${entityClassName}IntegrationSpec
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
          |  "${entityClassName}" must {
          |    val client: ${typeName(client)} =
          |      ${typeName(client)}(testKit.grpcClientSettings)
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
