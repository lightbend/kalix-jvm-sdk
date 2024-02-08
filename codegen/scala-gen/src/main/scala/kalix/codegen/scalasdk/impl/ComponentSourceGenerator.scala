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
import kalix.codegen.ProtoMessageType
import kalix.codegen.ModelBuilder
import kalix.codegen.ModelBuilder.ActionService
import kalix.codegen.ModelBuilder.EntityService
import kalix.codegen.ModelBuilder.Model
import kalix.codegen.ModelBuilder.Service
import kalix.codegen.ModelBuilder.ViewService
import kalix.codegen.PackageNaming

object ComponentSourceGenerator {
  import ScalaGeneratorUtils._
  import kalix.codegen.SourceGeneratorUtils._
  import Types._

  private final case class CallableComponent(
      uniqueName: String,
      service: ModelBuilder.Service,
      callableCommands: Iterable[ModelBuilder.Command])

  def generateManaged(model: Model, mainPackageName: PackageNaming): Iterable[File] = {
    // since we want to flatten component names to as short as possible there may be duplicate
    // names, so for those we need to use a longer name
    val services = model.services.values.toSeq
    val uniqueNamesAndComponents = services
      .flatMap { service =>
        val callableCommands = callableCommandsFor(service)
        if (callableCommands.nonEmpty) {
          val name = nameFor(service)
          // FIXME component.messageType.name is the gRPC service name, not the component class which is what we want
          if (services.exists(other => other != service && nameFor(other) == name)) {
            // give it a longer unique name
            val uniqueName =
              service.messageType.parent.javaPackage.replaceAllLiterally(".", "_") + "_" + service.messageType.name
            Some(CallableComponent(uniqueName, service, callableCommands))
          } else {
            Some(CallableComponent(name, service, callableCommands))
          }
        } else {
          None
        }
      }

    generateComponentsTrait(uniqueNamesAndComponents, mainPackageName) :: generateComponentsImpl(
      uniqueNamesAndComponents,
      mainPackageName) :: Nil
  }

  private def generateComponentsTrait(components: Seq[CallableComponent], mainPackageName: PackageNaming): File = {
    val componentCalls = components.map { component =>
      // FIXME only unary deferred calls supported for now, or could streamed out be supported if types align?
      // higher risk of name conflicts here where all components in the service meets up, so all
      // type names are fully qualified rather than imported
      val methods = component.callableCommands
        .map(command =>
          c"""def ${lowerFirst(command.name)}(command: ${fullyQualifiedClassWithRoot(
            command.inputType)}): $DeferredCall[${fullyQualifiedClassWithRoot(
            command.inputType)}, ${fullyQualifiedClassWithRoot(command.outputType)}]
             |""")

      c"""trait ${component.uniqueName}Calls {
         |  $methods
         |}"""
    }

    val componentGetters = components.map { component =>
      c"def ${lowerFirst(component.uniqueName)}: ${component.uniqueName}Calls"
    }

    generate(
      mainPackageName,
      "Components",
      c"""
         |$managedComment
         |
         |/**
         | * Not intended for user extension, provided through generated implementation
         | */
         |trait Components {
         | import Components._
         |
         | $componentGetters
         |
         |}
         |
         |object Components{
         |
         | $componentCalls
         |
         |}""")
  }

  private def generateComponentsImpl(components: Seq[CallableComponent], mainPackageName: PackageNaming): File = {
    val componentGetters = components.map { component =>
      c"""@Override
         |override def ${lowerFirst(component.uniqueName)}: Components.${component.uniqueName}Calls =
         |  new ${component.uniqueName}CallsImpl();
         |"""
    }

    val componentCallImpls = components.map { component =>
      val methods = component.callableCommands
        .map { command =>
          val commandMethod = lowerFirst(command.name)
          val inputType = fullyQualifiedClassWithRoot(command.inputType)
          val outputType = fullyQualifiedClassWithRoot(command.outputType)
          val messageType = component.service.messageType
          c"""override def $commandMethod(command: $inputType): $DeferredCall[$inputType, $outputType] =
             |  $ScalaDeferredCallAdapter(
             |    command,
             |    context.componentCallMetadata,
             |    "${component.service.messageType.fullyQualifiedProtoName}",
             |    "${command.name}",
             |    (metadata: Metadata) => {
             |      val client = getGrpcClient(classOf[_root_.${messageType.fullyQualifiedGrpcServiceInterfaceName}])
             |      if (client.isInstanceOf[_root_.${messageType.fullyQualifiedGrpcServiceInterfaceName}Client]) {
             |        addHeaders(
             |          client.asInstanceOf[_root_.${messageType.fullyQualifiedGrpcServiceInterfaceName}Client].$commandMethod(),
             |          metadata).invoke(command)
             |      } else {
             |        // only for tests with mocked client implementation
             |        client.$commandMethod(command)
             |      }
             |    })"""
        }

      c"""private final class ${component.uniqueName}CallsImpl extends Components.${component.uniqueName}Calls {
         |  $methods
         |}"""

    }
    generate(
      mainPackageName,
      "ComponentsImpl",
      c"""
         |$managedComment
         |
         |/**
         | * Not intended for direct instantiation, called by generated code, use Action.components() to access
         | */
         |final class ComponentsImpl(context: $InternalContext) extends Components {
         |
         |  def this(context: $Context) =
         |    this(context.asInstanceOf[$InternalContext])
         |
         |  private def getGrpcClient[T](serviceClass: Class[T]): T =
         |    context.getComponentGrpcClient(serviceClass)
         |
         |  private def addHeaders[Req, Res](
         |      requestBuilder: $SingleResponseRequestBuilder[Req, Res],
         |      metadata: $Metadata): $SingleResponseRequestBuilder[Req, Res] = {
         |    metadata.filter(_.isText).foldLeft(requestBuilder) { (builder, entry) =>
         |      builder.addHeader(entry.key, entry.value)
         |    }
         |  }
         |
         | $componentGetters
         |
         | $componentCallImpls
         |
         |}""")
  }

  private def nameFor(service: Service): String =
    service match {
      case as: ActionService => as.className.split('.').last
      case es: EntityService => es.componentFullName.split('.').last
      case vs: ViewService   => vs.className.split('.').last
    }

  private def fullyQualifiedMessage(messageType: ProtoMessageType): String =
    s"${messageType.parent.javaPackage}.${messageType.name}"

  private def callableCommandsFor(service: Service): Iterable[ModelBuilder.Command] =
    service match {
      case view: ViewService =>
        // only queries, not update commands for views
        view.queries.filter(_.isUnary)
      case _ =>
        // only unary commands for now
        service.commands.filter(_.isUnary)
    }

  private def fullyQualifiedClassWithRoot(name: ProtoMessageType): String =
    s"_root_.${name.fullyQualifiedName}"
}
