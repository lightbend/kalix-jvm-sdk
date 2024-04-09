/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.codegen

object Format {

  val break = "\n"

  def indent(lines: Iterable[String], num: Int): String =
    indent(lines.mkString(break), num)

  /*
   * Be mindful that `Format.indent` does not indent the first line,
   * so the invocation itself needs to be indented as required.
   *
   * Empty lines are not indented either.
   */
  def indent(str: String, num: Int): String = {
    str
      .split(break)
      .zipWithIndex
      .collect {
        // don't indent first line and empty lines
        case (line, idx) if idx == 0 || line.trim.isEmpty => line
        case (line, _)                                    => (" " * num) + line
      }
      .mkString(break)
  }

}
