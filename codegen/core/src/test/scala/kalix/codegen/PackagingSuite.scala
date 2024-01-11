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

package kalix.codegen

import kalix.codegen.TestData.protoMessageType

class PackagingSuite extends munit.FunSuite {
  private val testData = TestData.scalaStyle

  test("ProtoMessageType should be able to produce filenames") {
    val parent = testData.domainProto()
    val messageType = protoMessageType(parent, "MyClass")

    assertNoDiff(messageType.fileBasename + ".scala", "com/example/service/domain/MyClass.scala")
  }

  test("ClassMessageType should be able to produce filenames") {
    val messageType = ClassMessageType("com.example.service.domain", "MyClass")

    assertNoDiff(messageType.fileBasename + ".scala", "com/example/service/domain/MyClass.scala")
  }
}
