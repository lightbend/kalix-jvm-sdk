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
