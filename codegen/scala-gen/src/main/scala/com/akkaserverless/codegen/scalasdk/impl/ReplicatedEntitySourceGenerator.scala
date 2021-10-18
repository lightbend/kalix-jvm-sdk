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
import com.lightbend.akkasls.codegen.Imports
import com.lightbend.akkasls.codegen.Format
import com.lightbend.akkasls.codegen.ModelBuilder

object ReplicatedEntitySourceGenerator {

  import com.lightbend.akkasls.codegen.SourceGeneratorUtils._
  import ScalaGeneratorUtils._

  def generateUnmanaged(entity: ModelBuilder.ReplicatedEntity, service: ModelBuilder.EntityService): Seq[File] =
    Seq(generateImplementationSkeleton(entity, service))

  def generateManaged(entity: ModelBuilder.ReplicatedEntity, service: ModelBuilder.EntityService): Seq[File] =
    Seq(abstractEntity(entity, service), handler(entity, service), provider(entity, service))

  private[codegen] def generateImplementationSkeleton(
      entity: ModelBuilder.ReplicatedEntity,
      service: ModelBuilder.EntityService): File = {

    val packageName = entity.fqn.parent.scalaPackage
    val className = entity.fqn.name
    val abstractEntityName = entity.abstractEntityName

    implicit val imports: Imports = generateCommandAndTypeArgumentImports(
      service.commands,
      entity.data.typeArguments,
      packageName,
      otherImports = Seq(
        "com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntity",
        "com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityContext",
        s"com.akkaserverless.scalasdk.replicatedentity.${entity.data.name}") ++ extraReplicatedImports(entity.data),
      packageImports = Seq(service.fqn.parent.scalaPackage))

    val typeArguments = parameterizeDataType(entity.data)
    val parameterizedDataType = entity.data.name + typeArguments

    val methods = service.commands
      .map { cmd =>
        val methodName = cmd.name
        val inputType = cmd.inputType
        val outputType = cmd.outputType

        s"""|/** Command handler for "${cmd.name}". */
            |def ${lowerFirst(methodName)}(currentData: $parameterizedDataType, ${lowerFirst(
          cmd.inputType.name)}: ${typeName(inputType)}): ReplicatedEntity.Effect[${typeName(outputType)}] =
            |  effects.error("The command handler for `${cmd.name}` is not implemented, yet")
            |""".stripMargin
      }

    val emptyValue = entity.data match {
      case ModelBuilder.ReplicatedRegister(valueType) =>
        s"""|  override def emptyValue: ${dataType(valueType)} =
            |    throw new UnsupportedOperationException("Not implemented yet, replace with your empty register value")
            |
            |""".stripMargin
      case _ => ""
    }

    File(
      packageName,
      className,
      s"""|package $packageName
          |
          |${writeImports(imports)}
          |
          |$unmanagedComment
          |
          |/** A replicated entity. */
          |class ${className}(context: ReplicatedEntityContext) extends $abstractEntityName {
          |
          |$emptyValue
          |  ${Format.indent(methods, 2)}
          |
          |}
          |""".stripMargin)
  }

  private[codegen] def abstractEntity(
      entity: ModelBuilder.ReplicatedEntity,
      service: ModelBuilder.EntityService): File = {

    val packageName = entity.fqn.parent.scalaPackage
    val abstractEntityName = entity.abstractEntityName
    val baseClass = entity.data.baseClass

    implicit val imports: Imports = generateCommandAndTypeArgumentImports(
      service.commands,
      entity.data.typeArguments,
      packageName,
      otherImports = Seq(
        "com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntity",
        s"com.akkaserverless.scalasdk.replicatedentity.${entity.data.name}",
        s"com.akkaserverless.scalasdk.replicatedentity.$baseClass") ++ extraReplicatedImports(entity.data),
      packageImports = Seq(service.fqn.parent.scalaPackage))

    val typeArguments = parameterizeDataType(entity.data)
    val parameterizedDataType = entity.data.name + typeArguments

    val methods = service.commands
      .map { cmd =>
        val methodName = cmd.name
        val inputType = cmd.inputType
        val outputType = cmd.outputType

        s"""|/** Command handler for "${cmd.name}". */
            |def ${lowerFirst(methodName)}(currentData: $parameterizedDataType, ${lowerFirst(
          cmd.inputType.name)}: ${typeName(inputType)}): ReplicatedEntity.Effect[${typeName(outputType)}]
            |""".stripMargin
      }

    File(
      packageName,
      abstractEntityName,
      s"""|package $packageName
          |
          |${writeImports(imports)}
          |
          |$managedComment
          |
          |/** A replicated entity. */
          |abstract class ${abstractEntityName} extends $baseClass$typeArguments {
          |
          |  ${Format.indent(methods, 2)}
          |
          |}
          |""".stripMargin)
  }

  private[codegen] def handler(entity: ModelBuilder.ReplicatedEntity, service: ModelBuilder.EntityService): File = {

    val packageName = entity.fqn.parent.scalaPackage
    val routerName = entity.routerName

    implicit val imports: Imports = generateImports(
      service.commands.map(_.inputType),
      packageName,
      otherImports = Seq(
        "com.akkaserverless.javasdk.impl.replicatedentity.ReplicatedEntityRouter.CommandHandlerNotFound",
        "com.akkaserverless.scalasdk.impl.replicatedentity.ReplicatedEntityRouter",
        "com.akkaserverless.scalasdk.replicatedentity.CommandContext",
        "com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntity",
        s"com.akkaserverless.scalasdk.replicatedentity.${entity.data.name}") ++
        extraReplicatedImports(entity.data) ++
        extraTypeImports(entity.data.typeArguments),
      packageImports = Seq(service.fqn.parent.scalaPackage))

    val parameterizedDataType = entity.data.name + parameterizeDataType(entity.data)

    val commandCases = service.commands
      .map { cmd =>
        val methodName = cmd.name
        val inputType = typeName(cmd.inputType)
        s"""|case "$methodName" =>
            |  entity.${lowerFirst(methodName)}(data, command.asInstanceOf[$inputType])
            |""".stripMargin
      }

    File(
      packageName,
      routerName,
      s"""|package $packageName
          |
          |${writeImports(imports)}
          |
          |$managedComment
          |
          |/**
          | * A replicated entity handler that is the glue between the Protobuf service `${service.fqn.name}`
          | * and the command handler methods in the `${entity.fqn.name}` class.
          | */
          |class ${routerName}(entity: ${entity.fqn.name})
          |  extends ReplicatedEntityRouter[$parameterizedDataType, ${entity.fqn.name}](entity) {
          |
          |  override def handleCommand(
          |      commandName: String,
          |      data: $parameterizedDataType,
          |      command: Any,
          |      context: CommandContext): ReplicatedEntity.Effect[_] = {
          |
          |    commandName match {
          |      ${Format.indent(commandCases, 6)}
          |
          |      case _ =>
          |        throw new CommandHandlerNotFound(commandName)
          |    }
          |  }
          |}
          |""".stripMargin)
  }

  private[codegen] def provider(entity: ModelBuilder.ReplicatedEntity, service: ModelBuilder.EntityService): File = {

    val packageName = entity.fqn.parent.scalaPackage
    val providerName = entity.providerName

    val relevantTypes = {
      entity.data.typeArguments.collect { case ModelBuilder.MessageTypeArgument(fqn) =>
        fqn
      } ++ service.commands.flatMap { cmd =>
        cmd.inputType :: cmd.outputType :: Nil
      }
    }

    implicit val imports = generateImports(
      relevantTypes.map(_.descriptorImport),
      packageName,
      otherImports = Seq(
        s"com.akkaserverless.scalasdk.replicatedentity.${entity.data.name}",
        "com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityContext",
        "com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityOptions",
        "com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityProvider",
        "com.google.protobuf.Descriptors",
        "scala.collection.immutable") ++
        extraReplicatedImports(entity.data) ++
        extraTypeImports(entity.data.typeArguments),
      packageImports = Seq(service.fqn.parent.scalaPackage))

    val parameterizedDataType = entity.data.name + parameterizeDataType(entity.data)

    val relevantTypesToImport = collectRelevantTypes(relevantTypes, service.fqn)
    val descriptors =
      relevantTypesToImport
        .map(d => typeName(d.descriptorImport))
        .distinct
        .sorted

    File(
      packageName,
      providerName,
      s"""
         |package $packageName
         |
         |${writeImports(imports)}
         |
         |$managedComment
         |
         |/**
         | * A replicated entity provider that defines how to register and create the entity for
         | * the Protobuf service `${service.fqn.name}`.
         | *
         | * Should be used with the `register` method in [[com.akkaserverless.scalasdk.AkkaServerless]].
         | */
         |object $providerName {
         |  def apply(entityFactory: ReplicatedEntityContext => ${entity.fqn.name}): $providerName =
         |    new $providerName(entityFactory, ReplicatedEntityOptions.defaults)
         |
         |  def apply(entityFactory: ReplicatedEntityContext => ${entity.fqn.name}, options: ReplicatedEntityOptions): $providerName =
         |    new $providerName(entityFactory, options)
         |}
         |
         |
         |class $providerName private (
         |    entityFactory: ReplicatedEntityContext => ${entity.fqn.name},
         |    override val options: ReplicatedEntityOptions)
         |    extends ReplicatedEntityProvider[$parameterizedDataType, ${typeName(entity.fqn)}] {
         |
         |  override def entityType: String = "${entity.entityType}"
         |
         |  override def newRouter(context: ReplicatedEntityContext): ${entity.routerName} =
         |    new ${entity.routerName}(entityFactory(context))
         |
         |  override def serviceDescriptor: Descriptors.ServiceDescriptor =
         |    ${typeName(service.fqn.descriptorImport)}.javaDescriptor.findServiceByName("${service.fqn.protoName}")
         |
         |  override def additionalDescriptors: immutable.Seq[Descriptors.FileDescriptor] =
         |    ${descriptors.map(d => d + ".javaDescriptor :: ").toList.distinct.mkString}Nil
         |}
         |""".stripMargin)
  }

}
