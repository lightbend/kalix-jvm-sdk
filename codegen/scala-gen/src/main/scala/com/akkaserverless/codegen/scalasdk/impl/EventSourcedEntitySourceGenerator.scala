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
import com.lightbend.akkasls.codegen.FullyQualifiedName
import com.lightbend.akkasls.codegen.ModelBuilder
import com.lightbend.akkasls.codegen.PackageNaming

object EventSourcedEntitySourceGenerator {
  import ScalaGeneratorUtils._
  import com.lightbend.akkasls.codegen.SourceGeneratorUtils._

  def generateUnmanaged(
      eventSourcedEntity: ModelBuilder.EventSourcedEntity,
      service: ModelBuilder.EntityService): Seq[File] =
    Seq(generateImplementationSkeleton(eventSourcedEntity, service))

  def generateManaged(
      eventSourcedEntity: ModelBuilder.EventSourcedEntity,
      service: ModelBuilder.EntityService,
      mainPackageName: PackageNaming): Seq[File] =
    Seq(
      abstractEntity(eventSourcedEntity, service, mainPackageName),
      handler(eventSourcedEntity, service),
      provider(eventSourcedEntity, service))

  private[codegen] def abstractEntity(
      eventSourcedEntity: ModelBuilder.EventSourcedEntity,
      service: ModelBuilder.EntityService,
      mainPackageName: PackageNaming): File = {
    import Types.EventSourcedEntity._
    val abstractEntityName = eventSourcedEntity.abstractEntityName

    val stateType = eventSourcedEntity.state.fqn
    val commandHandlers = service.commands
      .map { cmd =>
        val methodName = cmd.name

        c"""|def ${lowerFirst(methodName)}(currentState: $stateType, ${lowerFirst(cmd.inputType.name)}: ${cmd.inputType}): $EventSourcedEntity.Effect[${cmd.outputType}]
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

    val Components = FullyQualifiedName.noDescriptor(mainPackageName.javaPackage + ".Components")
    val ComponentsImpl = FullyQualifiedName.noDescriptor(mainPackageName.javaPackage + ".ComponentsImpl")

    generate(
      eventSourcedEntity.fqn.parent,
      abstractEntityName,
      c"""|$managedComment
          |
          |abstract class $abstractEntityName extends $EventSourcedEntity[$stateType] {
          |
          |  def components: $Components =
          |    new ${ComponentsImpl}(commandContext())
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
    import Types.EventSourcedEntity._

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
    import Types.{ ImmutableSeq, Descriptors }
    import Types.EventSourcedEntity._
    val className = entity.providerName

    val descriptors =
      (Seq(entity.state.fqn) ++ (service.commands.map(_.inputType) ++ service.commands.map(
        _.outputType)) ++ entity.events.map(_.fqn))
        .map(_.descriptorImport)

    generate(
      entity.fqn.parent,
      entity.providerName,
      c"""|$managedComment
          |
          |object $className {
          |  def apply(entityFactory: $EventSourcedEntityContext => ${entity.fqn.name}): $className =
          |    new $className(entityFactory, $EventSourcedEntityOptions.defaults)
          |}
          |class $className private(entityFactory: $EventSourcedEntityContext => ${entity.fqn.name}, override val options: $EventSourcedEntityOptions)
          |  extends $EventSourcedEntityProvider[${entity.state.fqn}, ${entity.fqn}] {
          |
          |  def withOptions(newOptions: $EventSourcedEntityOptions): $className =
          |    new $className(entityFactory, newOptions)
          |
          |  override final val serviceDescriptor: $Descriptors.ServiceDescriptor =
          |    ${service.fqn.descriptorImport}.javaDescriptor.findServiceByName("${service.fqn.protoName}")
          |
          |  override final val entityType: String = "${entity.entityType}"
          |
          |  override final def newRouter(context: $EventSourcedEntityContext): ${entity.routerName} =
          |    new ${entity.routerName}(entityFactory(context))
          |
          |  override final val additionalDescriptors: $ImmutableSeq[$Descriptors.FileDescriptor] =
          |    ${descriptors.distinct.map(d => c"$d.javaDescriptor ::").toVector.distinct} Nil
          |}
          |""",
      packageImports = Seq(service.fqn.parent))
  }

  def generateImplementationSkeleton(
      eventSourcedEntity: ModelBuilder.EventSourcedEntity,
      service: ModelBuilder.EntityService): File = {
    import Types.EventSourcedEntity._

    val eventHandlers =
      eventSourcedEntity.events.map { event =>
        c"""|override def ${lowerFirst(event.fqn.name)}(currentState: ${eventSourcedEntity.state.fqn}, ${lowerFirst(
          event.fqn.name)}: ${event.fqn}): ${eventSourcedEntity.state.fqn} =
            |  throw new RuntimeException("The event handler for `${event.fqn.name}` is not implemented, yet")
            |"""
      }

    val commandHandlers =
      service.commands.map { cmd =>
        c"""|override def ${lowerFirst(cmd.name)}(currentState: ${eventSourcedEntity.state.fqn}, ${lowerFirst(
          cmd.inputType.name)}: ${cmd.inputType}): $EventSourcedEntity.Effect[${cmd.outputType}] =
            |  effects.error("The command handler for `${cmd.name}` is not implemented, yet")
            |"""
      }

    generate(
      eventSourcedEntity.fqn.parent,
      eventSourcedEntity.fqn.name,
      c"""|$unmanagedComment
          |
          |class ${eventSourcedEntity.fqn.name}(context: $EventSourcedEntityContext) extends ${eventSourcedEntity.abstractEntityName} {
          |  override def emptyState: ${eventSourcedEntity.state.fqn} =
          |    throw new UnsupportedOperationException("Not implemented yet, replace with your empty entity state")
          |
          |  $commandHandlers
          |  $eventHandlers
          |}""",
      packageImports = Seq(service.fqn.parent))
  }
}
