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
import com.lightbend.akkasls.codegen.TestData
import protocgen.CodeGenRequest
import scalapb.compiler.{ DescriptorImplicits, GeneratorParams }

class ValueEntitySourceGeneratorSuite extends munit.FunSuite {
  import com.akkaserverless.codegen.scalasdk.impl.ValueEntitySourceGenerator._

  test("it can generate a value entity implementation skeleton") {
    val file = generateImplementationSkeleton(TestData.valueEntity(), TestData.simpleEntityService())
    assertNoDiff(
      file.content,
      s"""package com.example.service.domain
         |
         |import com.example.service.GetValue
         |import com.example.service.MyState
         |import com.example.service.SetValue
         |import com.external.Empty
         |
         |class MyValueEntity /* extends AbstractMyValueEntity */ {
         |  def set(currentState: com.example.service.domain.MyState, command: SetValue): Empty = ???
         |
         |  def get(currentState: com.example.service.domain.MyState, command: GetValue): MyState = ???
         |}
         |""".stripMargin)
  }
}
