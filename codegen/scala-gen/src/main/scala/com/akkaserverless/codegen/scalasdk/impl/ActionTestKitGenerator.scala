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
import com.lightbend.akkasls.codegen.Format
import com.lightbend.akkasls.codegen.ModelBuilder

object ActionTestKitGenerator {

  import com.lightbend.akkasls.codegen.SourceGeneratorUtils._

  def generateUnmanagedTest(service: ModelBuilder.ActionService): Seq[File] =
    Seq(test(service))

  def generateManagedTest(service: ModelBuilder.ActionService): Seq[File] =
    Seq(testkit(service))

  private[codegen] def testkit(service: ModelBuilder.ActionService): File = {
    implicit val imports =
      generateImports(
        service.commands.map(_.inputType) ++
        service.commands.map(_.outputType),
        service.fqn.parent.scalaPackage,
        otherImports = Seq(
          "com.akkaserverless.scalasdk.testkit.ActionResult",
          "com.akkaserverless.scalasdk.testkit.impl.ActionResultImpl",
          "com.akkaserverless.scalasdk.action.ActionCreationContext",
          "com.akkaserverless.scalasdk.testkit.impl.TestKitActionContext"))

    val actionClassName = service.className

    val methods = service.commands.collect {
      case cmd if cmd.isUnary =>
        s"""|def ${lowerFirst(cmd.name)}(command: ${typeName(cmd.inputType)}): ActionResult[${typeName(cmd.outputType)}] =
          |  new ActionResultImpl(newActionInstance().${lowerFirst(cmd.name)}(command))
          |""".stripMargin
    }

    File(
      service.fqn.parent.scalaPackage,
      s"${actionClassName}TestKit",
      s"""|package ${service.fqn.parent.scalaPackage}
          |
          |${writeImports(imports, isScala = true)}
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
          |   */
          |  def apply(actionFactory: ActionCreationContext => $actionClassName): ${actionClassName}TestKit =
          |    new ${actionClassName}TestKit(actionFactory)
          |
          |}
          |
          |/**
          | * TestKit for unit testing $actionClassName
          | */
          |final class ${actionClassName}TestKit private(actionFactory: ActionCreationContext => $actionClassName) {
          |
          |  private def newActionInstance() = actionFactory(new TestKitActionContext)
          |
          |  ${Format.indent(methods, 2)}
          |}
          |""".stripMargin)
  }

  def test(service: ModelBuilder.ActionService): File = {

    implicit val imports =
      generateImports(
        service.commands.map(_.inputType) ++ service.commands.map(_.outputType),
        service.fqn.parent.scalaPackage,
        otherImports = Seq(
          "com.akkaserverless.scalasdk.action.Action",
          "com.akkaserverless.scalasdk.testkit.ActionResult",
          "org.scalatest.matchers.should.Matchers",
          "org.scalatest.wordspec.AnyWordSpec"))

    val actionClassName = service.className

    val testCases = service.commands.collect {
      case cmd if cmd.isUnary =>
        s"""|"handle command ${cmd.name}" in {
          |  val testKit = ${actionClassName}TestKit(new $actionClassName(_))
          |  // val result = testKit.${lowerFirst(cmd.name)}(${typeName(cmd.inputType)}(...))
          |}
          |""".stripMargin
    }

    File(
      service.fqn.parent.scalaPackage,
      actionClassName + "Spec",
      s"""|package ${service.fqn.parent.scalaPackage}
          |
          |${writeImports(imports, isScala = true)}
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
          |      val testKit = ${actionClassName}TestKit(new $actionClassName(_))
          |      // use the testkit to execute a command
          |      // and verify final updated state:
          |      // val result = testKit.someOperation(SomeRequest)
          |      // verify the response
          |      // result.reply shouldBe expectedReply
          |    }
          |
          |    ${Format.indent(testCases, 4)}
          |
          |  }
          |}
          |""".stripMargin)
  }

}
