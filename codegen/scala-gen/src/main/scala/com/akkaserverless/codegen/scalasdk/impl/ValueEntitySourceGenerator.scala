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

import com.lightbend.akkasls.codegen.File
import com.lightbend.akkasls.codegen.Imports
import com.lightbend.akkasls.codegen.Format
import com.lightbend.akkasls.codegen.FullyQualifiedName
import com.lightbend.akkasls.codegen.ModelBuilder
import com.lightbend.akkasls.codegen.PackageNaming

object ValueEntitySourceGenerator {
  import com.lightbend.akkasls.codegen.SourceGeneratorUtils._
  import ScalaGeneratorUtils._

  def generateUnmanaged(valueEntity: ModelBuilder.ValueEntity, service: ModelBuilder.EntityService): Seq[File] =
    Seq(generateImplementationSkeleton(valueEntity, service))

  def generateManaged(
      valueEntity: ModelBuilder.ValueEntity,
      service: ModelBuilder.EntityService,
      mainPackageName: PackageNaming): Seq[File] =
    Seq(
      abstractEntity(valueEntity, service, mainPackageName),
      handler(valueEntity, service),
      provider(valueEntity, service))

  private[codegen] def abstractEntity(
      valueEntity: ModelBuilder.ValueEntity,
      service: ModelBuilder.EntityService,
      mainPackageName: PackageNaming): File = {
    import Types.ValueEntity._

    val stateType = valueEntity.state.fqn
    val abstractEntityName = valueEntity.abstractEntityName

    val methods = service.commands
      .map { cmd =>
        val methodName = cmd.name

        val inputType = cmd.inputType
        val outputType = cmd.outputType

        c"""|/** Command handler for "${cmd.name}". */
            |def ${lowerFirst(methodName)}(currentState: $stateType, ${lowerFirst(cmd.inputType.name)}: $inputType): $ValueEntity.Effect[$outputType]
            |"""

      }

    val Components = FullyQualifiedName.noDescriptor(mainPackageName.javaPackage + ".Components")
    val ComponentsImpl = FullyQualifiedName.noDescriptor(mainPackageName.javaPackage + ".ComponentsImpl")

    generate(
      valueEntity.fqn.parent,
      abstractEntityName,
      c"""|$managedComment
          |
          |/** A value entity. */
          |abstract class $abstractEntityName extends $ValueEntity[$stateType] {
          |
          |  def components: $Components =
          |    new ${ComponentsImpl}(commandContext())
          |
          |  $methods
          |}
          |""",
      packageImports = Seq(service.fqn.parent))
  }

  private[codegen] def handler(valueEntity: ModelBuilder.ValueEntity, service: ModelBuilder.EntityService): File = {
    import Types.ValueEntity._
    val stateType = valueEntity.state.fqn
    val valueEntityName = valueEntity.fqn

    val commandCases = service.commands
      .map { cmd =>
        val methodName = cmd.name
        val inputType = cmd.inputType
        c"""|case "$methodName" =>
            |  entity.${lowerFirst(methodName)}(state, command.asInstanceOf[$inputType])
            |"""
      }

    generate(
      valueEntity.fqn.parent,
      valueEntity.routerName,
      c"""|$managedComment
          |
          |/**
          | * A value entity handler that is the glue between the Protobuf service <code>CounterService</code>
          | * and the command handler methods in the <code>Counter</code> class.
          | */
          |class ${valueEntityName.name}Router(entity: $valueEntityName) extends $ValueEntityRouter[$stateType, $valueEntityName](entity) {
          |  def handleCommand(commandName: String, state: $stateType, command: Any, context: $CommandContext): $ValueEntity.Effect[_] = {
          |    commandName match {
          |      $commandCases
          |      case _ =>
          |        throw new $CommandHandlerNotFound(commandName)
          |    }
          |  }
          |}
          |""",
      packageImports = Seq(service.fqn.parent))
  }

  def provider(entity: ModelBuilder.ValueEntity, service: ModelBuilder.EntityService): File = {
    import Types.{ ImmutableSeq, Descriptors }
    import Types.ValueEntity._
    val className = entity.providerName

    val descriptors =
      (Seq(entity.state.fqn) ++ (service.commands.map(_.inputType) ++ service.commands.map(_.outputType)))
        .map(_.descriptorImport)

    generate(
      entity.fqn.parent,
      className,
      c"""|$managedComment
          |
          |object $className {
          |  def apply(entityFactory: $ValueEntityContext => ${entity.fqn.name}): $className =
          |    new $className(entityFactory, $ValueEntityOptions.defaults)
          |}
          |class $className private(entityFactory: $ValueEntityContext => ${entity.fqn.name}, override val options: $ValueEntityOptions)
          |  extends $ValueEntityProvider[${entity.state.fqn}, ${entity.fqn}] {
          |
          |  def withOptions(newOptions: $ValueEntityOptions): $className =
          |    new $className(entityFactory, newOptions)
          |
          |  override final val serviceDescriptor: $Descriptors.ServiceDescriptor =
          |    ${service.fqn.descriptorImport}.javaDescriptor.findServiceByName("${service.fqn.protoName}")
          |
          |  override final val entityType = "${entity.entityType}"
          |
          |  override final def newRouter(context: $ValueEntityContext): ${entity.routerName} =
          |    new ${entity.routerName}(entityFactory(context))
          |
          |  override final val additionalDescriptors: $ImmutableSeq[$Descriptors.FileDescriptor] =
          |    ${descriptors.distinct.map(d => c"$d.javaDescriptor :: ")}Nil
          |}
          |""",
      packageImports = Seq(service.fqn.parent))
  }

  def generateImplementationSkeleton(
      valueEntity: ModelBuilder.ValueEntity,
      service: ModelBuilder.EntityService): File = {
    import Types.ValueEntity._

    val methods = service.commands.map { cmd =>
      c"""|override def ${lowerFirst(cmd.name)}(currentState: ${valueEntity.state.fqn}, ${lowerFirst(cmd.inputType.name)}: ${cmd.inputType}): $ValueEntity.Effect[${cmd.outputType}] =
          |  effects.error("The command handler for `${cmd.name}` is not implemented, yet")
          |"""
    }

    generate(
      valueEntity.fqn.parent,
      valueEntity.fqn.name,
      c"""|$unmanagedComment
          |
          |/** A value entity. */
          |class ${valueEntity.fqn.name}(context: $ValueEntityContext) extends ${valueEntity.abstractEntityName} {
          |  override def emptyState: ${valueEntity.state.fqn} =
          |    throw new UnsupportedOperationException("Not implemented yet, replace with your empty entity state")
          |
          |  $methods
          |}
          |""",
      packageImports = Seq(service.fqn.parent))
  }
}
