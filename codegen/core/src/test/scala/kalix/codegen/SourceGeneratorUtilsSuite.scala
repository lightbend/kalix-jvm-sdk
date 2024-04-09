/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.codegen

class SourceGeneratorUtilsSuite extends munit.FunSuite {
  import SourceGeneratorUtils._

  test("it can determine the main package") {
    assertNoDiff(mainPackageName(Set("com.lightbend.Something")).mkString("."), "com.lightbend")
    assertNoDiff(
      mainPackageName(Set("com.lightbend.Something", "com.lightbend.SomethingElse")).mkString("."),
      "com.lightbend")
    assertNoDiff(
      mainPackageName(Set("com.lightbend.Something", "com.lightbend.other.SomethingElse")).mkString("."),
      "com.lightbend")

    // no common package prefix should fail
    intercept[RuntimeException] {
      mainPackageName(Set("com.lightbend.Something", "io.akka.SomethingElse")).mkString(".")
    }
  }

}
