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

import com.akkaserverless.codegen.scalasdk.impl.ActionServiceSourceGenerator
import com.akkaserverless.codegen.scalasdk.impl.ActionTestKitGenerator
import com.lightbend.akkasls.codegen.TestData

class ActionTestKitGeneratorSuite extends munit.FunSuite {
  private val testData = TestData.scalaStyle
  test("it can generate a TestKit for an action") {
    val service = testData.simpleActionService()
    val generatedSrc =
      ActionTestKitGenerator.generateManagedTest(service).head.content

    assertNoDiff(
      generatedSrc,
      """package com.example.service
        |
        |import com.akkaserverless.scalasdk.action.ActionCreationContext
        |import com.akkaserverless.scalasdk.testkit.ActionResult
        |import com.akkaserverless.scalasdk.testkit.impl.ActionResultImpl
        |import com.akkaserverless.scalasdk.testkit.impl.TestKitActionContext
        |import com.external.Empty
        |
        |// This code is managed by Akka Serverless tooling.
        |// It will be re-generated to reflect any changes to your protobuf definitions.
        |// DO NOT EDIT
        |
        |/**
        | * TestKit for unit testing MyServiceAction
        | */
        |object MyServiceActionTestKit {
        |  /**
        |   * Create a testkit instance of MyServiceAction
        |   * @param entityFactory A function that creates a MyServiceAction based on the given ActionCreationContext
        |   */
        |  def apply(actionFactory: ActionCreationContext => MyServiceAction): MyServiceActionTestKit =
        |    new MyServiceActionTestKit(actionFactory)
        |
        |}
        |
        |/**
        | * TestKit for unit testing MyServiceAction
        | */
        |final class MyServiceActionTestKit private(actionFactory: ActionCreationContext => MyServiceAction) {
        |
        |  private def newActionInstance() = actionFactory(new TestKitActionContext)
        |
        |  def simpleMethod(command: MyRequest): ActionResult[Empty] =
        |    new ActionResultImpl(newActionInstance().simpleMethod(command))
        |}""".stripMargin)
  }

  test("it can generate a test stub for an action") {
    val service = testData.simpleActionService()
    val generatedSrc =
      ActionTestKitGenerator.generateUnmanagedTest(service).head.content

    assertNoDiff(
      generatedSrc,
      """package com.example.service
        |
        |import com.akkaserverless.scalasdk.action.Action
        |import com.akkaserverless.scalasdk.testkit.ActionResult
        |import com.external.Empty
        |import org.scalatest.matchers.should.Matchers
        |import org.scalatest.wordspec.AnyWordSpec
        |
        |// This class was initially generated based on the .proto definition by Akka Serverless tooling.
        |//
        |// As long as this file exists it will not be overwritten: you can maintain it yourself,
        |// or delete it so it is regenerated as needed.
        |
        |class MyServiceActionSpec
        |    extends AnyWordSpec
        |    with Matchers {
        |
        |  "MyServiceAction" must {
        |
        |    "have example test that can be removed" in {
        |      val testKit = MyServiceActionTestKit(new MyServiceAction(_))
        |      // use the testkit to execute a command
        |      // and verify final updated state:
        |      // val result = testKit.someOperation(SomeRequest)
        |      // verify the response
        |      // result.reply shouldBe expectedReply
        |    }
        |
        |    "handle command SimpleMethod" in {
        |      val testKit = MyServiceActionTestKit(new MyServiceAction(_))
        |      // val result = testKit.simpleMethod(MyRequest(...))
        |    }
        |
        |  }
        |}""".stripMargin)
  }
}
