/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

import kalix.codegen.Format

class FormatSpec extends munit.FunSuite {

  test("indenting should leave alone the first line") {

    val method = """|public String someName(){
                    |  return "hi";
                    |}""".stripMargin

    val obtained =
      s"""
        |a
        |
        |  ${Format.indent(method, 2)}
        |b
        """.stripMargin

    val expected =
      s"""
        |a
        |
        |  public String someName(){
        |    return "hi";
        |  }
        |b
        """.stripMargin

    assertEquals(obtained, expected)
  }

}
