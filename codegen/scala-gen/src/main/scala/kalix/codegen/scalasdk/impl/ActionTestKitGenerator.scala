/*
 * Copyright 2024 Lightbend Inc.
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

package kalix.codegen.scalasdk.impl

import kalix.codegen.File
import kalix.codegen.Format
import kalix.codegen.Imports
import kalix.codegen.ModelBuilder

object ActionTestKitGenerator {

  import kalix.codegen.SourceGeneratorUtils._
  import ScalaGeneratorUtils._

  def generateUnmanagedTest(service: ModelBuilder.ActionService): Seq[File] =
    Seq(test(service))

  def generateManagedTest(service: ModelBuilder.ActionService): Seq[File] =
    Seq(testkit(service))

  private[codegen] def testkit(service: ModelBuilder.ActionService): File = {
    val commands = service.commands.filterNot(_.ignore)
    implicit val imports: Imports =
      generateImports(
        commands.map(_.inputType) ++
        commands.map(_.outputType),
        service.messageType.parent.scalaPackage,
        otherImports = Seq(
          "kalix.scalasdk.Metadata",
          "kalix.scalasdk.testkit.ActionResult",
          "kalix.scalasdk.testkit.impl.ActionResultImpl",
          "kalix.scalasdk.action.ActionCreationContext",
          "kalix.scalasdk.testkit.impl.TestKitActionContext",
          "kalix.scalasdk.testkit.MockRegistry") ++ commandStreamedTypes(commands))

    val actionClassName = service.className

    val methods = commands.map { cmd =>
      val methodBeginning = if (cmd.handleDeletes) {
        s"""def ${lowerFirst(cmd.name)}(metadata: Metadata = Metadata.empty): ${selectOutputResult(cmd)} = {\n""" +
        "  val context = new TestKitActionContext(metadata, mockRegistry)\n"
      } else {
        s"""def ${lowerFirst(cmd.name)}(command: ${selectInput(
          cmd)}, metadata: Metadata = Metadata.empty): ${selectOutputResult(cmd)} = {\n""" +
        "  val context = new TestKitActionContext(metadata, mockRegistry)\n"
      }
      methodBeginning +
      (if (cmd.isUnary || cmd.isStreamIn) {
         if (cmd.handleDeletes) {
           s"""  new ActionResultImpl(newActionInstance(context).${lowerFirst(cmd.name)}())"""
         } else {
           s"""  new ActionResultImpl(newActionInstance(context).${lowerFirst(cmd.name)}(command))"""
         }
       } else {
         s"""  newActionInstance(context).${lowerFirst(
           cmd.name)}(command).map(effect => new ActionResultImpl(effect))"""
       }) + "\n" +
      "}\n"
    }

    File.scala(
      service.messageType.parent.scalaPackage,
      s"${actionClassName}TestKit",
      s"""|package ${service.messageType.parent.scalaPackage}
          |
          |${writeImports(imports)}
          |
          |$managedComment
          |
          |/**
          | * TestKit for unit testing $actionClassName
          | */
          |object ${actionClassName}TestKit {
          |  /**
          |   * Create a testkit instance of $actionClassName
          |   * @param entityFactory A function that creates a $actionClassName based on the given ActionCreationContext
          |   * @param mockRegistry A map of mocks (Class -> mock) that provides control and the ability to test the dependencies on another components / services
          |   */
          |  def apply(actionFactory: ActionCreationContext => $actionClassName, mockRegistry: MockRegistry = MockRegistry.empty): ${actionClassName}TestKit =
          |    new ${actionClassName}TestKit(actionFactory, mockRegistry)
          |
          |}
          |
          |/**
          | * TestKit for unit testing $actionClassName
          | */
          |final class ${actionClassName}TestKit private(actionFactory: ActionCreationContext => $actionClassName, mockRegistry: MockRegistry) {
          |
          |  private def newActionInstance(context: TestKitActionContext) = {
          |    val action = actionFactory(context)
          |    action._internalSetActionContext(Some(context))
          |    action
          |  }
          |
          |  ${Format.indent(methods, 2)}
          |}
          |""".stripMargin)
  }

  def test(service: ModelBuilder.ActionService): File = {
    val commands = service.commands.filterNot(_.ignore)
    implicit val imports: Imports =
      generateImports(
        commands.map(_.inputType) ++ commands.map(_.outputType),
        service.messageType.parent.scalaPackage,
        otherImports = Seq(
          "kalix.scalasdk.action.Action",
          "kalix.scalasdk.testkit.ActionResult",
          "org.scalatest.matchers.should.Matchers",
          "org.scalatest.wordspec.AnyWordSpec") ++ commandStreamedTypes(commands))

    val actionClassName = service.className

    val testCases = commands.map { cmd =>
      s""""handle command ${cmd.name}" in {\n""" +
      (if (cmd.isUnary || cmd.isStreamOut) {
         if (cmd.handleDeletes) {
           s"""|  val service = ${actionClassName}TestKit(new $actionClassName(_))
              |      pending
              |  // val result = service.${lowerFirst(cmd.name)}()
              |}
              |""".stripMargin
         } else {
           s"""|  val service = ${actionClassName}TestKit(new $actionClassName(_))
              |      pending
              |  // val result = service.${lowerFirst(cmd.name)}(${typeName(cmd.inputType)}(...))
              |}
              |""".stripMargin
         }
       } else {
         s"""|  val service = ${actionClassName}TestKit(new $actionClassName(_))
              |      pending
              |  // val result = service.${lowerFirst(cmd.name)}(Source.single(${typeName(cmd.inputType)}(...)))
              |}
              |""".stripMargin
       })
    }

    File.scala(
      service.messageType.parent.scalaPackage,
      actionClassName + "Spec",
      s"""|package ${service.messageType.parent.scalaPackage}
          |
          |${writeImports(imports)}
          |
          |$unmanagedComment
          |
          |class ${actionClassName}Spec
          |    extends AnyWordSpec
          |    with Matchers {
          |
          |  "${actionClassName}" must {
          |
          |    "have example test that can be removed" in {
          |      val service = ${actionClassName}TestKit(new $actionClassName(_))
          |      pending
          |      // use the testkit to execute a command
          |      // and verify final updated state:
          |      // val result = service.someOperation(SomeRequest)
          |      // verify the reply
          |      // result.reply shouldBe expectedReply
          |    }
          |
          |    ${Format.indent(testCases, 4)}
          |
          |  }
          |}
          |""".stripMargin)
  }

  def selectOutputResult(command: ModelBuilder.Command)(implicit imports: Imports): String = {
    if (command.streamedOutput)
      s"Source[ActionResult[${typeName(command.outputType)}], akka.NotUsed]"
    else s"ActionResult[${typeName(command.outputType)}]"
  }

  def selectOutputEffect(command: ModelBuilder.Command)(implicit imports: Imports): String = {
    if (command.streamedOutput)
      s"Source[Effect[${typeName(command.outputType)}], akka.NotUsed]"
    else s"Effect[${typeName(command.outputType)}]"
  }

  def selectOutputReturn(command: ModelBuilder.Command): String = {
    if (command.streamedOutput) "effect.map(interpretEffects);"
    else "interpretEffects(effect);"
  }

  def selectInput(command: ModelBuilder.Command)(implicit imports: Imports): String = {
    if (command.streamedInput) s"Source[${typeName(command.inputType)}, akka.NotUsed]"
    else typeName(command.inputType)
  }

  def commandStreamedTypes(commands: Iterable[ModelBuilder.Command]): Seq[String] = {
    if (commands.exists(c => c.streamedInput || c.streamedOutput)) Seq("akka.stream.scaladsl.Source", "akka.NotUsed")
    else Nil
  }

}
