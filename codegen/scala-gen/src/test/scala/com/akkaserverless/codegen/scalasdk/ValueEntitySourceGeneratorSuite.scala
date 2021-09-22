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

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import com.lightbend.akkasls.codegen.{ PackageNaming, TestData }
import protocgen.CodeGenRequest
import scalapb.compiler.{ DescriptorImplicits, GeneratorParams }

class ValueEntitySourceGeneratorSuite extends munit.FunSuite {
  import com.akkaserverless.codegen.scalasdk.impl.ValueEntitySourceGenerator._

  // TODO use package naming template parameter to generate Scala-style testData
  private val testData = TestData()

  val domainParent = PackageNaming("domain.proto", "", "com.example.service.domain", None, None, false)
  val apiParent = PackageNaming("api.proto", "", "com.example.service", None, None, false)

  test("it can generate a value entity implementation skeleton") {
    val file =
      generateImplementationSkeleton(testData.valueEntity(domainParent), testData.simpleEntityService(apiParent))
    assertNoDiff(
      file.content,
      s"""package com.example.service.domain
         |
         |import com.akkaserverless.scalasdk.valueentity.ValueEntity
         |import com.akkaserverless.scalasdk.valueentity.ValueEntityContext
         |import com.example.service
         |import com.external.Empty
         |
         |/** A value entity. */
         |class MyValueEntity(context: ValueEntityContext) extends AbstractMyValueEntity {
         |  override def emptyState: MyState =
         |    throw new UnsupportedOperationException("Not implemented yet, replace with your empty entity state")
         |
         |  override def set(currentState: MyState, command: service.SetValue): ValueEntity.Effect[Empty] =
         |    effects.error("The command handler for `Set` is not implemented, yet");
         |
         |  override def get(currentState: MyState, command: service.GetValue): ValueEntity.Effect[service.MyState] =
         |    effects.error("The command handler for `Get` is not implemented, yet");
         |}
         |""".stripMargin)
  }
  test("it can generate an abstract value entity implementation") {
    val str = abstractEntity(testData.valueEntity(domainParent), testData.simpleEntityService(apiParent))
    assertNoDiff(
      str,
      s"""package com.example.service.domain
          |
          |import com.akkaserverless.scalasdk.valueentity.ValueEntity
          |import com.example.service
          |import com.external.Empty
          |
          |/** A value entity. */
          |abstract class AbstractMyValueEntity extends ValueEntity[MyState] {
          |
          |  /** Command handler for "Set". */
          |  def set(currentState: MyState, setValue: service.SetValue): ValueEntity.Effect[Empty]
          |
          |  /** Command handler for "Get". */
          |  def get(currentState: MyState, getValue: service.GetValue): ValueEntity.Effect[service.MyState]
          |}
          |""".stripMargin)
  }

  test("it can generate a value entity handler implementation") {
    val str = handler(testData.valueEntity(domainParent), testData.simpleEntityService(apiParent))
    assertNoDiff(
      str,
      s"""package com.example.service.domain
          |
          |import com.akkaserverless.scalasdk.impl.valueentity.ValueEntityHandler
          |import com.akkaserverless.scalasdk.impl.valueentity.ValueEntityHandler.CommandHandlerNotFound
          |import com.akkaserverless.scalasdk.valueentity.CommandContext
          |import com.akkaserverless.scalasdk.valueentity.ValueEntity
          |import com.example.service
          |import com.external.Empty
          |
          |/**
          | * A value entity handler that is the glue between the Protobuf service <code>CounterService</code>
          | * and the command handler methods in the <code>Counter</code> class.
          | */
          |class MyValueEntityHandler(entity: MyValueEntity) extends ValueEntityHandler[MyState, MyValueEntity](entity) {
          |  def handleCommand(commandName: String, state: MyState, command: Any, context: CommandContext): ValueEntity.Effect[_] = {
          |    commandName match {
          |      case "Set" =>
          |        entity.set(state, command.asInstanceOf[service.SetValue])
          |
          |      case "Get" =>
          |        entity.get(state, command.asInstanceOf[service.GetValue])
          |
          |      case _ =>
          |        throw new ValueEntityHandler.CommandHandlerNotFound(commandName)
          |    }
          |  }
          |}
          |""".stripMargin)
  }

  test("it can generate a provider") {
    val file =
      provider(testData.valueEntity(domainParent), testData.simpleEntityService(apiParent))
    assertNoDiff(file.content, "")
  }
}
