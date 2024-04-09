/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.codegen.java

import kalix.codegen._
import kalix.codegen.java.JavaGeneratorUtils._

class JavaGeneratorUtilsSuite extends munit.FunSuite {
  test("refer to a FQN with an outer class name when it is not imported") {
    implicit val imports = new Imports("com.example", Nil)

    val packaging = PackageNaming(protoFileName = "test.proto", name = "Outer", protoPackage = "com.example.domain")
    val messageType = ProtoMessageType.noDescriptor("Test", packaging)

    // It's not imported, so we should use the full name, including outer class:
    assertEquals(typeName(messageType), "com.example.domain.Outer.Test")
  }

  test("refer to an inner class directly when the outer class is imported") {
    implicit val imports = new Imports("com.example", Nil)

    val packaging = PackageNaming(protoFileName = "test.proto", name = "Outer", protoPackage = "com.example.domain")
    val messageType = ProtoMessageType.noDescriptor("Test", packaging)

    // It's not imported, so we should use the full name, including outer class:
    assertEquals(typeName(messageType), "com.example.domain.Outer.Test")
  }

  test("refer to an inner class via its outer class when the outer class is in the current package") {
    implicit val imports = new Imports("com.example", Nil)

    val packaging = PackageNaming(protoFileName = "test.proto", name = "Outer", protoPackage = "com.example")
    val messageType = ProtoMessageType.noDescriptor("Test", packaging)

    // It's not imported, so we should use the full name, including outer class:
    assertEquals(typeName(messageType), "Outer.Test")
  }

  test("refer to a class with its FQN when there would be clashing imports") {
    implicit val imports = new Imports("com.example", Seq("com.example.bar.Foo", "com.example.baz.Foo"))
    val bar =
      PackageNaming(protoFileName = "test.proto", name = "", protoPackage = "com.example.bar", javaMultipleFiles = true)
    val messageType = ProtoMessageType.noDescriptor("Foo", bar)

    assertEquals(typeName(messageType), "com.example.bar.Foo")
    assert(!writeImports(imports).contains("com.example.bar.Foo"))
    assert(!writeImports(imports).contains("com.example.bar.Bar"))
  }

  test("refer to a class with its FQN when there would be a clashing import") {
    implicit val imports = new Imports("com.example", Seq("com.example.bar.Foo", "com.example.Foo"))
    val bar =
      PackageNaming(protoFileName = "test.proto", name = "", protoPackage = "com.example.bar", javaMultipleFiles = true)
    val messageType = ProtoMessageType.noDescriptor("Foo", bar)

    assertEquals(typeName(messageType), "com.example.bar.Foo")
    assert(!writeImports(imports).contains("com.example.bar.Foo"))
  }
}
