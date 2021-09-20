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

package com.akkaserverless.codegen.scalasdk.impl

import com.akkaserverless.codegen.scalasdk.File
import com.lightbend.akkasls.codegen.ModelBuilder
import com.lightbend.akkasls.codegen.Syntax

object ValueEntitySourceGenerator {
  def generateImplementationSkeleton(entity: ModelBuilder.ValueEntity, service: ModelBuilder.EntityService): File = {
    val stateType = entity.state.fqn.fullQualifiedName
    println(s"commands: ${service.commands.map(_.outputType)}")
    val methods = service.commands.map { cmd =>
      // TODO 'override' and use 'effect'
      s"""|def ${lowerFirst(cmd.fqn.name)}(currentState: $stateType, command: ${cmd.inputType.fullQualifiedName}): ${cmd.outputType.fullQualifiedName} = ???
         |""".stripMargin
    }
    val imports =
      Set(stateType) ++
      service.commands.map(_.inputType.name) ++
      service.commands.map(_.outputType.name).map {
        case "Empty" => "com.external.Empty"
        case other   => other
      }

    File(
      entity.fqn.fileBasename + ".scala",
      s"""
         |package ${entity.fqn.parent.javaPackage}
         |
         |${imports.map(i => s"import $i").mkString("\n")}
         |
         |class ${entity.fqn.name} /* extends Abstract${entity.fqn.name} */ {
         |  ${Syntax.indent(methods, 2)}
         |}
         |""".stripMargin)
  }

  private def lowerFirst(text: String): String =
    text.headOption match {
      case Some(c) => c.toLower.toString + text.drop(1)
      case None    => ""
    }
}
