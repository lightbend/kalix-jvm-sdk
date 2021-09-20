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

import com.google.protobuf.Descriptors
import com.akkaserverless.codegen.scalasdk.File
import com.lightbend.akkasls.codegen.ModelBuilder
import com.lightbend.akkasls.codegen.Format
import com.lightbend.akkasls.codegen.FullyQualifiedName
import scalapb.compiler.DescriptorImplicits
import scalapb.descriptors.ScalaType

object ValueEntitySourceGenerator {
  import com.lightbend.akkasls.codegen.SourceGeneratorUtils._

  def generateImplementationSkeleton(entity: ModelBuilder.ValueEntity, service: ModelBuilder.EntityService)(implicit
      di: DescriptorImplicits): File = {
    val stateType = fullName(entity.state.fqn)
    val methods = service.commands.map { cmd =>
      // TODO 'override' and use 'effect'
//      s"""|def ${lowerFirst(cmd.fqn.name)}(currentState: $stateType, command: ${cmd.inputType.fullQualifiedName}): ${cmd.outputType.fullQualifiedName} = ???
//          |""".stripMargin
      s"""|def ${lowerFirst(cmd.fqn.name)}(currentState: Unit, command: Unit): Unit = ???
          |""".stripMargin
    }
    val imports = Set.empty
    Set(stateType)
//      ++ service.commands.map(_.inputType.fullQualifiedName) ++
//      service.commands.map(_.outputType.fullQualifiedName)

    File(
      entity.fqn.fileBasename + ".scala",
      s"""
         |package ${entity.fqn.parent.javaPackage}
         |
         |${imports.map(i => s"import $i").mkString("\n")}
         |
         |class ${entity.fqn.name} /* extends Abstract${entity.fqn.name} */ {
         |  ${Format.indent(methods, 2)}
         |}
         |""".stripMargin)
  }

  private def fullName(fqn: FullyQualifiedName)(implicit di: DescriptorImplicits): String =
    fullName(fqn.descriptor)

  private def fullName(descriptor: Descriptors.GenericDescriptor)(implicit di: DescriptorImplicits): String = {
    import di._
    descriptor match {
      case d: Descriptors.Descriptor =>
        d.scalaType.fullName
    }
  }
}
