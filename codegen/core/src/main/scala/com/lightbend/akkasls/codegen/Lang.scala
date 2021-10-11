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

package com.lightbend.akkasls.codegen

sealed trait Lang {
  def suffix: String
  def wildcard: String
  def intType: String
  def wrapTypeParams(types: Iterable[String]): String

  def writeImports(imports: Imports): String =
    imports.imports
      .map { imported =>
        if (imported == "com.google.protobuf.any.Any") {
          s"import com.google.protobuf.any.{ Any => ScalaPbAny }${suffix}"
        } else
          s"import $imported${suffix}"
      }
      .mkString("\n")
}

object Scala extends Lang {
  override def suffix: String = ""
  override def wildcard: String = "_"
  override def intType: String = "Int"

  override def wrapTypeParams(types: Iterable[String]): String =
    types.mkString("[", ", ", "]")
}

object Java extends Lang {
  override def suffix: String = ";"
  override def wildcard: String = "?"
  override def intType: String = "Integer"

  override def wrapTypeParams(types: Iterable[String]): String =
    types.mkString("<", ", ", ">")
}
