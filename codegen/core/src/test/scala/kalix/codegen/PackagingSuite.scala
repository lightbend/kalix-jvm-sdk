/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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
