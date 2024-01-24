/*
 * Copyright 2024 Lightbend Inc.
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

package kalix.codegen.scalasdk.impl

import kalix.codegen.File
import kalix.codegen.Format
import kalix.codegen.Imports
import kalix.codegen.ModelBuilder
import kalix.codegen.ClassMessageType
import kalix.codegen.ProtoMessageType

object ReplicatedEntitySourceGenerator {

  import ScalaGeneratorUtils._
  import kalix.codegen.SourceGeneratorUtils._

  def generateUnmanaged(entity: ModelBuilder.ReplicatedEntity, service: ModelBuilder.EntityService): Seq[File] =
    Seq(generateImplementationSkeleton(entity, service))

  def generateManaged(entity: ModelBuilder.ReplicatedEntity, service: ModelBuilder.EntityService): Seq[File] =
    Seq(abstractEntity(entity, service), handler(entity, service), provider(entity, service))

  private[codegen] def generateImplementationSkeleton(
      entity: ModelBuilder.ReplicatedEntity,
      service: ModelBuilder.EntityService): File = {

    val packageName = entity.messageType.parent.scalaPackage
    val className = entity.messageType.name
    val abstractEntityName = entity.abstractEntityName

    implicit val imports: Imports = generateCommandAndTypeArgumentImports(
      service.commands,
      entity.data.typeArguments,
      packageName,
      otherImports = Seq(
        "kalix.scalasdk.replicatedentity.ReplicatedEntity",
        "kalix.scalasdk.replicatedentity.ReplicatedEntityContext",
        s"kalix.scalasdk.replicatedentity.${entity.data.name}") ++ extraReplicatedImports(entity.data),
      packageImports = Seq(service.messageType.parent.scalaPackage))

    val typeArguments = parameterizeDataType(entity.data)
    val parameterizedDataType = entity.data.name + typeArguments

    val methods = service.commands
      .map { cmd =>
        val methodName = cmd.name
        val inputType = cmd.inputType
        val outputType = cmd.outputType

        s"""|def ${lowerFirst(methodName)}(currentData: $parameterizedDataType, ${lowerFirst(
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

    File.scala(
      packageName,
      className,
      s"""|package $packageName
          |
          |${writeImports(imports)}
          |
          |$unmanagedComment
          |
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

    val packageName = entity.messageType.parent.scalaPackage
    val abstractEntityName = entity.abstractEntityName
    val baseClass = entity.data.baseClass

    implicit val imports: Imports = generateCommandAndTypeArgumentImports(
      service.commands,
      entity.data.typeArguments,
      packageName,
      otherImports = Seq(
        "kalix.scalasdk.replicatedentity.ReplicatedEntity",
        s"kalix.scalasdk.replicatedentity.${entity.data.name}",
        s"kalix.scalasdk.replicatedentity.$baseClass") ++ extraReplicatedImports(entity.data),
      packageImports = Seq(service.messageType.parent.scalaPackage))

    val typeArguments = parameterizeDataType(entity.data)
    val parameterizedDataType = entity.data.name + typeArguments

    val methods = service.commands
      .map { cmd =>
        val methodName = cmd.name
        val inputType = cmd.inputType
        val outputType = cmd.outputType

        s"""|def ${lowerFirst(methodName)}(currentData: $parameterizedDataType, ${lowerFirst(
          cmd.inputType.name)}: ${typeName(inputType)}): ReplicatedEntity.Effect[${typeName(outputType)}]
            |""".stripMargin
      }

    File.scala(
      packageName,
      abstractEntityName,
      s"""|package $packageName
          |
          |${writeImports(imports)}
          |
          |$managedComment
          |
          |abstract class ${abstractEntityName} extends $baseClass$typeArguments {
          |
          |  ${Format.indent(methods, 2)}
          |
          |}
          |""".stripMargin)
  }

  private[codegen] def handler(entity: ModelBuilder.ReplicatedEntity, service: ModelBuilder.EntityService): File = {

    val packageName = entity.messageType.parent.scalaPackage
    val routerName = entity.routerName

    implicit val imports: Imports = generateImports(
      service.commands.map(_.inputType),
      packageName,
      otherImports = Seq(
        "kalix.javasdk.impl.replicatedentity.ReplicatedEntityRouter.CommandHandlerNotFound",
        "kalix.scalasdk.impl.replicatedentity.ReplicatedEntityRouter",
        "kalix.scalasdk.replicatedentity.CommandContext",
        "kalix.scalasdk.replicatedentity.ReplicatedEntity",
        s"kalix.scalasdk.replicatedentity.${entity.data.name}") ++
        extraReplicatedImports(entity.data) ++
        extraTypeImports(entity.data.typeArguments),
      packageImports = Seq(service.messageType.parent.scalaPackage))

    val parameterizedDataType = entity.data.name + parameterizeDataType(entity.data)

    val commandCases = service.commands
      .map { cmd =>
        val methodName = cmd.name
        val inputType = typeName(cmd.inputType)
        s"""|case "$methodName" =>
            |  entity.${lowerFirst(methodName)}(data, command.asInstanceOf[$inputType])
            |""".stripMargin
      }

    File.scala(
      packageName,
      routerName,
      s"""|package $packageName
          |
          |${writeImports(imports)}
          |
          |$managedComment
          |
          |/**
          | * A replicated entity handler that is the glue between the Protobuf service `${service.messageType.name}`
          | * and the command handler methods in the `${entity.messageType.name}` class.
          | */
          |class ${routerName}(entity: ${entity.messageType.name})
          |  extends ReplicatedEntityRouter[$parameterizedDataType, ${entity.messageType.name}](entity) {
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

    val packageName = entity.messageType.parent.scalaPackage
    val providerName = entity.providerName

    val relevantTypes = allRelevantMessageTypes(service, entity)
    val relevantProtoTypes = relevantTypes.collect { case proto: ProtoMessageType => proto }

    implicit val imports = generateImports(
      relevantProtoTypes.map(_.descriptorImport),
      packageName,
      otherImports = Seq(
        s"kalix.scalasdk.replicatedentity.${entity.data.name}",
        "kalix.scalasdk.replicatedentity.ReplicatedEntityContext",
        "kalix.scalasdk.replicatedentity.ReplicatedEntityOptions",
        "kalix.scalasdk.replicatedentity.ReplicatedEntityProvider",
        "com.google.protobuf.Descriptors",
        "scala.collection.immutable.Seq") ++
        extraReplicatedImports(entity.data) ++
        extraTypeImports(entity.data.typeArguments),
      packageImports = Seq(service.messageType.parent.scalaPackage))

    val parameterizedDataType = entity.data.name + parameterizeDataType(entity.data)

    val descriptors =
      collectRelevantTypes(relevantProtoTypes, service.messageType)
        .map { messageType => typeName(messageType.descriptorImport) }
        .distinct
        .sorted

    File.scala(
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
         | * the Protobuf service `${service.messageType.name}`.
         | *
         | * Should be used with the `register` method in [[kalix.scalasdk.Kalix]].
         | */
         |object $providerName {
         |  def apply(entityFactory: ReplicatedEntityContext => ${entity.messageType.name}): $providerName =
         |    new $providerName(entityFactory, ReplicatedEntityOptions.defaults)
         |
         |  def apply(entityFactory: ReplicatedEntityContext => ${entity.messageType.name}, options: ReplicatedEntityOptions): $providerName =
         |    new $providerName(entityFactory, options)
         |}
         |
         |
         |class $providerName private (
         |    entityFactory: ReplicatedEntityContext => ${entity.messageType.name},
         |    override val options: ReplicatedEntityOptions)
         |    extends ReplicatedEntityProvider[$parameterizedDataType, ${typeName(entity.messageType)}] {
         |
         |  override final val typeId: String = "${entity.typeId}"
         |
         |  override def newRouter(context: ReplicatedEntityContext): ${entity.routerName} =
         |    new ${entity.routerName}(entityFactory(context))
         |
         |  override def serviceDescriptor: Descriptors.ServiceDescriptor =
         |    ${typeName(service.messageType.descriptorImport)}.javaDescriptor.findServiceByName("${service.messageType.protoName}")
         |
         |  override def additionalDescriptors: Seq[Descriptors.FileDescriptor] =
         |    ${descriptors.map(d => d + ".javaDescriptor :: ").toList.distinct.mkString}Nil
         |}
         |""".stripMargin)
  }

}
