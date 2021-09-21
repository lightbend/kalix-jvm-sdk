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

  val domainParent = PackageNaming("domain.proto", "", "com.example.service.domain", None, None, None, false)
  val apiParent = PackageNaming("api.proto", "", "com.example.service", None, None, None, false)

  test("it can generate a value entity implementation skeleton") {
    val file =
      generateImplementationSkeleton(TestData.valueEntity(domainParent), TestData.simpleEntityService(apiParent))
    assertNoDiff(
      file.content,
      s"""package com.example.service.domain
         |
         |import com.example.service
         |import com.external.Empty
         |
         |class MyValueEntity /* extends AbstractMyValueEntity */ {
         |  def set(currentState: MyState, command: service.SetValue): Empty = ???
         |
         |  def get(currentState: MyState, command: service.GetValue): service.MyState = ???
         |}
         |""".stripMargin)
  }
}
