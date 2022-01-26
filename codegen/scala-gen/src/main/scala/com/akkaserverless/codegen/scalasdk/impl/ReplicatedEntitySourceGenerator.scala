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
import com.lightbend.akkasls.codegen.Format
import com.lightbend.akkasls.codegen.Imports
import com.lightbend.akkasls.codegen.ModelBuilder
import com.lightbend.akkasls.codegen.PojoMessageType
import com.lightbend.akkasls.codegen.ProtoMessageType

object ReplicatedEntitySourceGenerator {

  import ScalaGeneratorUtils._
  import com.lightbend.akkasls.codegen.SourceGeneratorUtils._

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
        "com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntity",
        "com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityContext",
        s"com.akkaserverless.scalasdk.replicatedentity.${entity.data.name}") ++ extraReplicatedImports(entity.data),
      packageImports = Seq(service.messageType.parent.scalaPackage))

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

    File.scala(
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

    val packageName = entity.messageType.parent.scalaPackage
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
      packageImports = Seq(service.messageType.parent.scalaPackage))

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

    File.scala(
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

    val packageName = entity.messageType.parent.scalaPackage
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

    import Types._
    import Types.ReplicatedEntity._
    val className = entity.providerName

    val relevantTypes = allRelevantMessageTypes(service, entity)
    val relevantProtoTypes = relevantTypes.collect { case proto: ProtoMessageType => proto }
    val relevantPojoTypes = relevantTypes.collect { case pojo: PojoMessageType => pojo }

    val parameterizedDataType =
      c"${lookupReplicatedData(entity.data.name)}${parameterizeDataTypeCodeBlock(entity.data)}"
    val relevantDescriptors = relevantProtoTypes.map(_.descriptorImport).distinct

    generate(
      entity.messageType.parent,
      className,
      c"""|$managedComment
          |
          |/**
          | * A replicated entity provider that defines how to register and create the entity for
          | * the Protobuf service `${service.messageType.name}`.
          | *
          | * Should be used with the `register` method in [[com.akkaserverless.scalasdk.AkkaServerless]].
          | */
          |object $className {
          |  def apply(entityFactory: $ReplicatedEntityContext => ${entity.messageType.name}): $className =
          |    new $className(entityFactory, $ReplicatedEntityOptions.defaults)
          |
          |  def apply(entityFactory: $ReplicatedEntityContext => ${entity.messageType.name}, options: $ReplicatedEntityOptions): $className =
          |    new $className(entityFactory, options)
          |}
          |
          |class $className private (
          |    entityFactory: $ReplicatedEntityContext => ${entity.messageType.name},
          |    override val options: $ReplicatedEntityOptions)
          |    extends $ReplicatedEntityProvider[$parameterizedDataType, ${entity.messageType}] {
          |
          |  override def entityType: String = "${entity.entityType}"
          |
          |  override def newRouter(context: $ReplicatedEntityContext): ${entity.routerName} =
          |    new ${entity.routerName}(entityFactory(context))
          |
          |  override def serviceDescriptor: $Descriptors.ServiceDescriptor =
          |    ${service.messageType.descriptorImport}.javaDescriptor.findServiceByName("${service.messageType.protoName}")
          |
          |  override def additionalDescriptors: $ImmutableSeq[$Descriptors.FileDescriptor] =
          |    ${relevantDescriptors.map(d => c"$d.javaDescriptor :: ")}Nil
          |    
          |  override def serializer: $Serializer = 
          |    ${generateSerializers(relevantPojoTypes)}
          |}
          |""")

  }

}
