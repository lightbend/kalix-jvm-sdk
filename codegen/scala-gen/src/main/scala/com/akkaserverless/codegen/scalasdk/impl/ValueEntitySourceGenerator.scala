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

  /**
   * Generate Scala sources the user view source file.
   */
  def generateUnmanaged(valueEntity: ModelBuilder.ValueEntity, service: ModelBuilder.EntityService): Iterable[File] =
    Seq(generateImplementationSkeleton(valueEntity, service))

  /**
   * Generate Scala sources for provider, handler, abstract baseclass for a view.
   */
  def generateManaged(valueEntity: ModelBuilder.ValueEntity, service: ModelBuilder.EntityService): Iterable[File] = {
    val generatedSources = Seq.newBuilder[File]

    val packageName = valueEntity.fqn.parent.scalaPackage
    val packagePath = packageAsPath(packageName)

    generatedSources += File(
      s"$packagePath/${valueEntity.abstractEntityName}.scala",
      abstractEntity(valueEntity, service))
    //FIXME add Handler and Provider

    generatedSources.result()
  }

  private[codegen] def abstractEntity(
      valueEntity: ModelBuilder.ValueEntity,
      service: ModelBuilder.EntityService): String = {
    val stateType = valueEntity.state.fqn.name
    val abstractEntityName = valueEntity.abstractEntityName
    implicit val imports =
      generateImports(
        Seq(valueEntity.state.fqn) ++
        service.commands.map(_.inputType) ++
        service.commands.map(_.outputType),
        valueEntity.fqn.parent.scalaPackage,
        otherImports = Seq("com.akkaserverless.scalasdk.valueentity.ValueEntity"),
        packageImports = Seq(service.fqn.parent.scalaPackage),
        semi = false)

    val methods = service.commands
      .map { cmd =>
        val methodName = cmd.name

        val inputType = typeName(cmd.inputType)
        val outputType = typeName(cmd.outputType)

        s"""|/** Command handler for "${cmd.name}". */
            |def ${lowerFirst(methodName)}(currentState: $stateType, ${lowerFirst(cmd.inputType.name)}: $inputType): ValueEntity.Effect[$outputType]
            |""".stripMargin

      }

    s"""|package ${valueEntity.fqn.parent.scalaPackage}
        |
        |$imports
        |
        |/** A value entity. */
        |abstract class $abstractEntityName extends ValueEntity[$stateType] {
        |
        |  ${Format.indent(methods, 2)}
        |}
        |""".stripMargin
  }

  def generateImplementationSkeleton(
      valueEntity: ModelBuilder.ValueEntity,
      service: ModelBuilder.EntityService): File = {
    implicit val imports: Imports =
      generateImports(
        Seq(valueEntity.state.fqn) ++
        service.commands.map(_.inputType) ++
        service.commands.map(_.outputType),
        valueEntity.fqn.parent.scalaPackage,
        otherImports = Seq("com.akkaserverless.scalasdk.valueentity.ValueEntity"),
        packageImports = Seq(service.fqn.parent.scalaPackage),
        semi = false)

    val methods = service.commands.map { cmd =>
      s"""|override def ${lowerFirst(cmd.name)}(currentState: ${typeName(valueEntity.state.fqn)}, command: ${typeName(
        cmd.inputType)}): ValueEntity.Effect[${typeName(cmd.outputType)}] =
          |  effects.error("The command handler for `${cmd.name}` is not implemented, yet");
          |""".stripMargin
    }

    File(
      valueEntity.fqn.fileBasename + ".scala",
      s"""
         |package ${valueEntity.fqn.parent.scalaPackage}
         |
         |$imports
         |
         |/** A value entity. */
         |class ${valueEntity.fqn.name}(val entityId: String) extends ${valueEntity.abstractEntityName} {
         |  ${Format.indent(methods, 2)}
         |
         |  override def emptyState =
         |    throw new UnsupportedOperationException("Not implemented yet, replace with your empty entity state")
         |}
         |object ${valueEntity.fqn.name} {
         |  def apply(context: Unit /* ValueEntityContext */): ${valueEntity.fqn.name} =
         |    new ${valueEntity.fqn.name}("TODO get id from context")
         |}
         |""".stripMargin)
  }
}
