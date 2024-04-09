/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.codegen.scalasdk.impl

import kalix.codegen.File
import kalix.codegen.Format
import kalix.codegen.ProtoMessageType
import kalix.codegen.Imports
import kalix.codegen.ModelBuilder

object ValueEntityTestKitGenerator {
  import kalix.codegen.SourceGeneratorUtils._
  import ScalaGeneratorUtils._

  def generateUnmanagedTest(
      main: ProtoMessageType,
      valueEntity: ModelBuilder.ValueEntity,
      service: ModelBuilder.EntityService): Seq[File] =
    Seq(test(valueEntity, service), integrationTest(main, service))

  def generateManagedTest(valueEntity: ModelBuilder.ValueEntity, service: ModelBuilder.EntityService): Seq[File] =
    Seq(testkit(valueEntity, service))

  private[codegen] def testkit(valueEntity: ModelBuilder.ValueEntity, service: ModelBuilder.EntityService): File = {
    implicit val imports: Imports =
      generateImports(
        Seq(valueEntity.state.messageType) ++
        service.commands.map(_.inputType) ++
        service.commands.map(_.outputType),
        valueEntity.messageType.parent.scalaPackage,
        otherImports = Seq(
          "kalix.scalasdk.Metadata",
          "kalix.scalasdk.testkit.ValueEntityResult",
          "kalix.scalasdk.testkit.impl.ValueEntityResultImpl",
          "kalix.scalasdk.valueentity.ValueEntity",
          "kalix.scalasdk.valueentity.ValueEntityContext",
          "kalix.scalasdk.testkit.impl.TestKitValueEntityCommandContext",
          "kalix.scalasdk.testkit.impl.TestKitValueEntityContext"),
        packageImports = Seq(service.messageType.parent.scalaPackage))

    val entityClassName = valueEntity.messageType.name

    val methods = service.commands.map { cmd =>
      s"""|def ${lowerFirst(cmd.name)}(command: ${typeName(
        cmd.inputType)}, metadata: Metadata = Metadata.empty): ValueEntityResult[${typeName(cmd.outputType)}] = {
          |  entity._internalSetCommandContext(Some(new TestKitValueEntityCommandContext(entityId = entityId, metadata = metadata)))
          |  val effect = entity.${lowerFirst(cmd.name)}(state, command)
          |  interpretEffects(effect)
          |}
          |""".stripMargin
    }

    File(
      valueEntity.messageType.fileBasename + "TestKit.scala",
      s"""|package ${valueEntity.messageType.parent.scalaPackage}
          |
          |${writeImports(imports)}
          |
          |$managedComment
          |
          |/**
          | * TestKit for unit testing $entityClassName
          | */
          |object ${entityClassName}TestKit {
          |  /**
          |   * Create a testkit instance of $entityClassName
          |   * @param entityFactory A function that creates a $entityClassName based on the given ValueEntityContext,
          |   *                      a default entity id is used.
          |   */
          |  def apply(entityFactory: ValueEntityContext => $entityClassName): ${entityClassName}TestKit =
          |    apply("testkit-entity-id", entityFactory)
          |
          |  /**
          |   * Create a testkit instance of $entityClassName with a specific entity id.
          |   */
          |  def apply(entityId: String, entityFactory: ValueEntityContext => $entityClassName): ${entityClassName}TestKit =
          |    new ${entityClassName}TestKit(entityFactory(new TestKitValueEntityContext(entityId)), entityId)
          |}
          |
          |/**
          | * TestKit for unit testing $entityClassName
          | */
          |final class ${entityClassName}TestKit private(entity: $entityClassName, entityId: String) {
          |  private var state: ${typeName(valueEntity.state.messageType)} = entity.emptyState
          |
          |  /**
          |   * @return The current state of the $entityClassName under test
          |   */
          |  def currentState(): ${typeName(valueEntity.state.messageType)} =
          |    state
          |
          |  private def interpretEffects[Reply](effect: ValueEntity.Effect[Reply]): ValueEntityResult[Reply] = {
          |    val result = new ValueEntityResultImpl[Reply](effect)
          |    if (result.stateWasUpdated)
          |      this.state = result.updatedState.asInstanceOf[${typeName(valueEntity.state.messageType)}]
          |    else if (result.stateWasDeleted)
          |      this.state = entity.emptyState
          |    result
          |  }
          |
          |  ${Format.indent(methods, 2)}
          |}
          |""".stripMargin)
  }

  def test(valueEntity: ModelBuilder.ValueEntity, service: ModelBuilder.EntityService): File = {

    implicit val imports: Imports =
      generateImports(
        Seq(valueEntity.state.messageType) ++
        service.commands.map(_.inputType) ++
        service.commands.map(_.outputType),
        valueEntity.messageType.parent.scalaPackage,
        otherImports = Seq(
          "kalix.scalasdk.valueentity.ValueEntity",
          "kalix.scalasdk.testkit.ValueEntityResult",
          "org.scalatest.matchers.should.Matchers",
          "org.scalatest.wordspec.AnyWordSpec"),
        packageImports = Seq(service.messageType.parent.scalaPackage))

    val entityClassName = valueEntity.messageType.name

    val testCases = service.commands.map { cmd =>
      s"""|"handle command ${cmd.name}" in {
          |  val service = ${entityClassName}TestKit(new $entityClassName(_))
          |  pending
          |  // val result = service.${lowerFirst(cmd.name)}(${typeName(cmd.inputType)}(...))
          |}
          |""".stripMargin
    }

    File(
      valueEntity.messageType.fileBasename + "Spec.scala",
      s"""|package ${valueEntity.messageType.parent.scalaPackage}
          |
          |${writeImports(imports)}
          |
          |class ${entityClassName}Spec
          |    extends AnyWordSpec
          |    with Matchers {
          |
          |  "${entityClassName}" must {
          |
          |    "have example test that can be removed" in {
          |      val service = ${entityClassName}TestKit(new $entityClassName(_))
          |      pending
          |      // use the testkit to execute a command
          |      // and verify final updated state:
          |      // val result = service.someOperation(SomeRequest)
          |      // verify the reply
          |      // val reply = result.getReply()
          |      // reply shouldBe expectedReply
          |      // verify the final state after the command
          |      // service.currentState() shouldBe expectedState
          |    }
          |
          |    ${Format.indent(testCases, 4)}
          |
          |  }
          |}
          |""".stripMargin)
  }

  def integrationTest(main: ProtoMessageType, service: ModelBuilder.EntityService): File = {

    implicit val imports: Imports =
      generateImports(
        Seq(main) ++
        service.commands.map(_.inputType) ++
        service.commands.map(_.outputType),
        service.messageType.parent.scalaPackage,
        otherImports = Seq(
          "akka.actor.ActorSystem",
          "kalix.scalasdk.testkit.KalixTestKit",
          "org.scalatest.matchers.should.Matchers",
          "org.scalatest.wordspec.AnyWordSpec",
          "org.scalatest.BeforeAndAfterAll",
          "org.scalatest.concurrent.ScalaFutures",
          "org.scalatest.time.Span",
          "org.scalatest.time.Seconds",
          "org.scalatest.time.Millis"),
        packageImports = Nil)

    val entityClassName = service.messageType.name

    File(
      service.messageType.fileBasename + "IntegrationSpec.scala",
      s"""|package ${service.messageType.parent.scalaPackage}
          |
          |${writeImports(imports)}
          |
          |$unmanagedComment
          |
          |class ${entityClassName}IntegrationSpec
          |    extends AnyWordSpec
          |    with Matchers
          |    with BeforeAndAfterAll
          |    with ScalaFutures {
          |
          |  implicit private val patience: PatienceConfig =
          |    PatienceConfig(Span(5, Seconds), Span(500, Millis))
          |
          |  private val testKit = KalixTestKit(Main.createKalix()).start()
          |
          |  private val client = testKit.getGrpcClient(classOf[${typeName(service.messageType)}])
          |
          |  "${entityClassName}" must {
          |
          |    "have example test that can be removed" in {
          |      pending
          |      // use the gRPC client to send requests to the
          |      // Kalix Runtime and verify the results
          |    }
          |
          |  }
          |
          |  override def afterAll(): Unit = {
          |    testKit.stop()
          |    super.afterAll()
          |  }
          |}
          |""".stripMargin)
  }
}
