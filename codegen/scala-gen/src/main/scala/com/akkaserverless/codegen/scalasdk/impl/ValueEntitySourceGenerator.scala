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
import com.lightbend.akkasls.codegen.Format

object ValueEntitySourceGenerator {
  import com.lightbend.akkasls.codegen.SourceGeneratorUtils._

  def generateImplementationSkeleton(entity: ModelBuilder.ValueEntity, service: ModelBuilder.EntityService): File = {
    implicit val imports =
      generateImports(
        Seq(entity.state.fqn) ++
        service.commands.map(_.inputType) ++
        service.commands.map(_.outputType),
        entity.fqn.parent.scalaPackage,
        otherImports = Seq.empty,
        packageImports = Seq(service.fqn.parent.scalaPackage),
        semi = false)

    val methods = service.commands.map { cmd =>
      // TODO 'override', use 'effect' for output, use actual state type for state
      s"""|def ${lowerFirst(cmd.name)}(currentState: ${typeName(entity.state.fqn)}, command: ${typeName(
        cmd.inputType)}): ${typeName(cmd.outputType)} = ???
          |""".stripMargin
    }

    File(
      entity.fqn.fileBasename + ".scala",
      s"""
         |package ${entity.fqn.parent.scalaPackage}
         |
         |$imports
         |
         |class ${entity.fqn.name} /* extends Abstract${entity.fqn.name} */ {
         |  ${Format.indent(methods, 2)}
         |}
         |""".stripMargin)
  }
}
