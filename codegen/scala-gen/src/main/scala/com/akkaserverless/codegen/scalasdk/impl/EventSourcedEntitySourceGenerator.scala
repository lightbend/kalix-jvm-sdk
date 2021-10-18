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
import com.lightbend.akkasls.codegen.Format
import com.lightbend.akkasls.codegen.Imports
import com.lightbend.akkasls.codegen.ModelBuilder

object EventSourcedEntitySourceGenerator {
  import com.lightbend.akkasls.codegen.SourceGeneratorUtils._
  import ScalaGeneratorUtils._

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

    val abstractEntityName = eventSourcedEntity.abstractEntityName

    val stateType = eventSourcedEntity.state.fqn
    val commandHandlers = service.commands
      .map { cmd =>
        val methodName = cmd.name

        c"""|def ${lowerFirst(methodName)}(currentState: $stateType, ${lowerFirst(cmd.inputType.name)}: ${cmd.inputType}): EventSourcedEntity.Effect[${cmd.outputType}]
            |"""
      }

    val eventHandlers =
      eventSourcedEntity match {
        case ModelBuilder.EventSourcedEntity(_, _, _, events) =>
          events.map { event =>
            c"""|def ${lowerFirst(event.fqn.name)}(currentState: $stateType, ${lowerFirst(
              event.fqn.name)}: ${event.fqn}): $stateType"""
          }
      }

    import Types._

    generate(
      eventSourcedEntity.fqn.parent,
      abstractEntityName,
      c"""|$managedComment
          |
          |/** An event sourced entity. */
          |abstract class $abstractEntityName extends $EventSourcedEntity[$stateType] {
          |
          |  $commandHandlers
          |  $eventHandlers
          |}
          |""",
      packageImports = Seq(service.fqn.parent))
  }

  private[codegen] def handler(
      eventSourcedEntity: ModelBuilder.EventSourcedEntity,
      service: ModelBuilder.EntityService): File = {
    import Types._

    val stateType = eventSourcedEntity.state.fqn
    val eventSourcedEntityName = eventSourcedEntity.fqn

    val eventCases = eventSourcedEntity.events.map { evt =>
      c"""|case evt: ${evt.fqn} =>
          |  entity.${lowerFirst(evt.fqn.name)}(state, evt)
          |"""
    }

    val commandCases = service.commands
      .map { cmd =>
        val methodName = cmd.name
        c"""|case "$methodName" =>
            |  entity.${lowerFirst(methodName)}(state, command.asInstanceOf[${cmd.inputType}])
            |"""
      }
    generate(
      eventSourcedEntity.fqn.parent,
      eventSourcedEntity.routerName,
      c"""|$managedComment
          |
          |/**
          | * An event sourced entity handler that is the glue between the Protobuf service <code>CounterService</code>
          | * and the command handler methods in the <code>Counter</code> class.
          | */
          |class ${eventSourcedEntityName}Router(entity: ${eventSourcedEntityName}) extends $EventSourcedEntityRouter[$stateType, $eventSourcedEntityName](entity) {
          |  def handleCommand(commandName: String, state: $stateType, command: Any, context: $CommandContext): $EventSourcedEntity.Effect[_] = {
          |    commandName match {
          |      $commandCases
          |      case _ =>
          |        throw new $CommandHandlerNotFound(commandName)
          |    }
          |  }
          |  def handleEvent(state: $stateType, event: Any): $stateType = {
          |    event match {
          |      $eventCases
          |      case _ =>
          |        throw new $EventHandlerNotFound(event.getClass)
          |    }
          |  }
          |}
          |""",
      packageImports = Seq(service.fqn.parent))
  }

  def provider(entity: ModelBuilder.EventSourcedEntity, service: ModelBuilder.EntityService): File = {
    val packageName = entity.fqn.parent.scalaPackage
    val className = entity.providerName

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
        "com.google.protobuf.Descriptors",
        "scala.collection.immutable.Seq"),
      packageImports = Seq(service.fqn.parent.scalaPackage))

    File(
      s"${packageAsPath(packageName)}/${className}.scala",
      s"""
         |package $packageName
         |
         |${writeImports(imports)}
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
         |  override final def newRouter(context: EventSourcedEntityContext): ${entity.routerName} =
         |    new ${entity.routerName}(entityFactory(context))
         |
         |  override final val additionalDescriptors: Seq[Descriptors.FileDescriptor] =
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
        packageImports = Seq(service.fqn.parent.scalaPackage))
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
      s"""package ${eventSourcedEntity.fqn.parent.scalaPackage}
         |
         |${writeImports(imports)}
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
