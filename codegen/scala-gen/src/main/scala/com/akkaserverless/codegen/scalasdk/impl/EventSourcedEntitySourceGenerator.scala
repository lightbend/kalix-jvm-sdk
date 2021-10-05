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

object EventSourcedEntitySourceGenerator {
  import com.lightbend.akkasls.codegen.SourceGeneratorUtils._

  def generateUnmanaged(
      eventSourcedEntity: ModelBuilder.EventSourcedEntity,
      service: ModelBuilder.EntityService): Seq[File] =
    Seq(generateImplementationSkeleton(eventSourcedEntity, service))

  def generateManaged(
      eventSourcedEntity: ModelBuilder.EventSourcedEntity,
      service: ModelBuilder.EntityService): Seq[File] =
    Seq(
      abstractEntity(eventSourcedEntity, service),
      handler(eventSourcedEntity, service),
      provider(eventSourcedEntity, service))

  private[codegen] def abstractEntity(
      eventSourcedEntity: ModelBuilder.EventSourcedEntity,
      service: ModelBuilder.EntityService): File = {
    val stateType = eventSourcedEntity.state.fqn.name
    val abstractEntityName = eventSourcedEntity.abstractEntityName

    implicit val imports =
      generateImports(
        Seq(eventSourcedEntity.state.fqn) ++
        service.commands.map(_.inputType) ++
        service.commands.map(_.outputType),
        eventSourcedEntity.fqn.parent.scalaPackage,
        otherImports = Seq("com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity"),
        packageImports = Seq(service.fqn.parent.scalaPackage),
        semi = false)

    val commandHandlers = service.commands
      .map { cmd =>
        val methodName = cmd.name

        val inputType = typeName(cmd.inputType)
        val outputType = typeName(cmd.outputType)

        s"""|def ${lowerFirst(methodName)}(currentState: $stateType, ${lowerFirst(cmd.inputType.name)}: $inputType): EventSourcedEntity.Effect[$outputType]
            |""".stripMargin

      }

    val eventHandlers =
      eventSourcedEntity match {
        case ModelBuilder.EventSourcedEntity(_, _, _, events) =>
          events.map { event =>
            s"""|def ${lowerFirst(event.fqn.name)}(currentState: ${typeName(
              eventSourcedEntity.state.fqn)}, ${lowerFirst(event.fqn.name)}: ${typeName(event.fqn)}): ${typeName(
              eventSourcedEntity.state.fqn)}""".stripMargin
          }
      }

    File(
      eventSourcedEntity.fqn.parent.scalaPackage,
      abstractEntityName,
      s"""|package ${eventSourcedEntity.fqn.parent.scalaPackage}
        |
        |$imports
        |
        |$managedComment
        |
        |/** An event sourced entity. */
        |abstract class $abstractEntityName extends EventSourcedEntity[$stateType] {
        |
        |  ${Format.indent(commandHandlers, 2)}
        |
        |  ${Format.indent(eventHandlers, 2)}
        |}
        |""".stripMargin)
  }

  private[codegen] def handler(
      eventSourcedEntity: ModelBuilder.EventSourcedEntity,
      service: ModelBuilder.EntityService): File = {
    val stateType = eventSourcedEntity.state.fqn.name
    val packageName = eventSourcedEntity.fqn.parent.scalaPackage
    val eventSourcedEntityName = eventSourcedEntity.fqn.name
    implicit val imports =
      generateImports(
        Seq(eventSourcedEntity.state.fqn) ++
        service.commands.map(_.inputType),
        eventSourcedEntity.fqn.parent.scalaPackage,
        otherImports = Seq(
          "com.akkaserverless.scalasdk.eventsourcedentity.CommandContext",
          "com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity",
          "com.akkaserverless.scalasdk.impl.eventsourcedentity.EventSourcedEntityHandler",
          "com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityHandler.CommandHandlerNotFound",
          "com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityHandler.EventHandlerNotFound"),
        packageImports = Seq(service.fqn.parent.scalaPackage),
        semi = false)

    val eventCases = eventSourcedEntity.events.map { evt =>
      val eventType = typeName(evt.fqn)
      s"""|case evt: $eventType =>
              |  entity.${lowerFirst(evt.fqn.name)}(state, evt)
              |""".stripMargin
    }

    val commandCases = service.commands
      .map { cmd =>
        val methodName = cmd.name
        val inputType = typeName(cmd.inputType)
        s"""|case "$methodName" =>
            |  entity.${lowerFirst(methodName)}(state, command.asInstanceOf[$inputType])
            |""".stripMargin
      }

    File(
      packageName,
      eventSourcedEntityName + "Handler",
      s"""|package $packageName
        |
        |$imports
        |
        |$managedComment
        |
        |/**
        | * An event sourced entity handler that is the glue between the Protobuf service <code>CounterService</code>
        | * and the command handler methods in the <code>Counter</code> class.
        | */
        |class ${eventSourcedEntityName}Handler(entity: ${eventSourcedEntityName}) extends EventSourcedEntityHandler[$stateType, ${eventSourcedEntityName}](entity) {
        |  def handleEvent(state: $stateType, event: Any): $stateType = {
        |    event match {
        |      ${Format.indent(eventCases, 6)}
        |
        |      case _ =>
        |        throw new EventHandlerNotFound(event.getClass)
        |    }
        |  }
        |  def handleCommand(commandName: String, state: $stateType, command: Any, context: CommandContext): EventSourcedEntity.Effect[_] = {
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

  def provider(entity: ModelBuilder.EventSourcedEntity, service: ModelBuilder.EntityService): File = {
    val packageName = entity.fqn.parent.scalaPackage
    val className = entity.fqn.name + "Provider"

    val descriptors =
      (Seq(entity.state.fqn) ++ (service.commands.map(_.inputType) ++ service.commands.map(_.outputType)))
        .map(_.descriptorImport)
    implicit val imports: Imports = generateImports(
      Seq(entity.state.fqn) ++ descriptors,
      packageName,
      Seq(
        "com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityContext",
        "com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityOptions",
        "com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityProvider",
        "com.google.protobuf.Descriptors"),
      packageImports = Seq(service.fqn.parent.scalaPackage),
      semi = false)

    File(
      s"${packageAsPath(packageName)}/${className}.scala",
      s"""
         |package $packageName
         |
         |$imports
         |
         |$managedComment
         |
         |object $className {
         |  def apply(entityFactory: EventSourcedEntityContext => ${entity.fqn.name}): $className =
         |    new $className(entityFactory, EventSourcedEntityOptions.defaults)
         |}
         |class $className private(entityFactory: EventSourcedEntityContext => ${entity.fqn.name}, override val options: EventSourcedEntityOptions)
         |  extends EventSourcedEntityProvider[${typeName(entity.state.fqn)}, ${typeName(entity.fqn)}] {
         |
         |  def withOptions(newOptions: EventSourcedEntityOptions): $className =
         |    new $className(entityFactory, newOptions)
         |
         |  override final val serviceDescriptor: Descriptors.ServiceDescriptor =
         |    ${typeName(service.fqn.descriptorImport)}.javaDescriptor.findServiceByName("${service.fqn.protoName}")
         |
         |  override final val entityType = "${entity.entityType}"
         |
         |  override final def newHandler(context: EventSourcedEntityContext): ${entity.handlerName} =
         |    new ${entity.handlerName}(entityFactory(context))
         |
         |  override final val additionalDescriptors =
         |    ${descriptors.map(d => typeName(d) + ".javaDescriptor :: ").toList.distinct.mkString}Nil
         |}
         |""".stripMargin)
  }

  def generateImplementationSkeleton(
      eventSourcedEntity: ModelBuilder.EventSourcedEntity,
      service: ModelBuilder.EntityService): File = {
    implicit val imports: Imports =
      generateImports(
        Seq(eventSourcedEntity.state.fqn) ++
        service.commands.map(_.inputType) ++
        service.commands.map(_.outputType),
        eventSourcedEntity.fqn.parent.scalaPackage,
        otherImports = Seq(
          "com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity",
          "com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityContext"),
        packageImports = Seq(service.fqn.parent.scalaPackage),
        semi = false)
    val eventHandlers =
      eventSourcedEntity match {
        case ModelBuilder.EventSourcedEntity(_, _, _, events) =>
          events.map { event =>
            s"""|override def ${lowerFirst(event.fqn.name)}(currentState: ${typeName(
              eventSourcedEntity.state.fqn)}, ${lowerFirst(event.fqn.name)}: ${typeName(event.fqn)}): ${typeName(
              eventSourcedEntity.state.fqn)} =
                |  throw new RuntimeException("The event handler for `${event.fqn.name}` is not implemented, yet")
                |""".stripMargin
          }
      }

    val commandHandlers = service.commands.map { cmd =>
      s"""|override def ${lowerFirst(cmd.name)}(currentState: ${typeName(eventSourcedEntity.state.fqn)}, ${lowerFirst(
        cmd.inputType.name)}: ${typeName(cmd.inputType)}): EventSourcedEntity.Effect[${typeName(cmd.outputType)}] =
          |  effects.error("The command handler for `${cmd.name}` is not implemented, yet")
          |""".stripMargin
    }

    File(
      eventSourcedEntity.fqn.fileBasename + ".scala",
      s"""
         |package ${eventSourcedEntity.fqn.parent.scalaPackage}
         |
         |$imports
         |
         |$unmanagedComment
         |
         |/** An event sourced entity. */
         |class ${eventSourcedEntity.fqn.name}(context: EventSourcedEntityContext) extends ${eventSourcedEntity.abstractEntityName} {
         |  override def emptyState: ${typeName(eventSourcedEntity.state.fqn)} =
         |    throw new UnsupportedOperationException("Not implemented yet, replace with your empty entity state")
         |
         |  ${Format.indent(commandHandlers, 2)}
         |
         |  ${Format.indent(eventHandlers, 2)}
         |}
         |""".stripMargin)
  }
}
