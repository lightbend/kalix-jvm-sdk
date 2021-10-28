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
import com.lightbend.akkasls.codegen.FullyQualifiedName
import com.lightbend.akkasls.codegen.ModelBuilder.ActionService
import com.lightbend.akkasls.codegen.ModelBuilder.EntityService
import com.lightbend.akkasls.codegen.ModelBuilder.Model
import com.lightbend.akkasls.codegen.ModelBuilder.Service
import com.lightbend.akkasls.codegen.ModelBuilder.ViewService
import com.lightbend.akkasls.codegen.PackageNaming

object ComponentSourceGenerator {
  import ScalaGeneratorUtils._
  import com.lightbend.akkasls.codegen.SourceGeneratorUtils._
  import Types._

  def generateManaged(model: Model, mainPackageName: PackageNaming): Iterable[File] = {
    // since we want to flatten component names to as short as possible there may be duplicate
    // names, so for those we need to use a longer name
    val services = model.services.values.toSeq
    val uniqueNamesAndComponents = services
      .filter(_.commands.exists(_.isUnary)) // only include if there is at least one unary
      .map { component =>
        val name = nameFor(component)
        // FIXME component.fqn.name is the gRPC service name, not the component class which is what we want
        if (services.exists(other => other != component && nameFor(other) == name)) {
          // give it a longer unique name
          component.fqn.parent.javaPackage.replaceAllLiterally(".", "_") + "_" + component.fqn.name -> component
        } else {
          name -> component
        }
      }
      .toMap

    generateComponentsTrait(uniqueNamesAndComponents, mainPackageName) :: generateComponentsImpl(
      uniqueNamesAndComponents,
      mainPackageName) :: Nil
  }

  private def generateComponentsTrait(services: Map[String, Service], mainPackageName: PackageNaming): File = {
    val componentCalls = services.map { case (name, component) =>
      // FIXME only unary deferred calls supported for now, or could streamed out be supported if types align?
      // higher risk of name conflicts here where all components in the service meets up, so all
      // type names are fully qualified rather than imported
      val unaryCommands = component.commands.filter(_.isUnary)
      val methods = unaryCommands
        .map(command =>
          c"""def ${lowerFirst(command.name)}(command: ${command.inputType.fullyQualifiedJavaName}): $DeferredCall[${command.inputType.fullyQualifiedJavaName}, ${command.outputType.fullyQualifiedJavaName}]
             |""")

      c"""trait ${name}Calls {
         |  $methods
         |}"""
    }

    val componentGetters = services.map { case (name, _) =>
      c"def ${lowerFirst(name)}: ${name}Calls"
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

  private def generateComponentsImpl(services: Map[String, Service], mainPackageName: PackageNaming): File = {
    val componentGetters = services.map { case (name, _) =>
      c"""@Override
         |override def ${lowerFirst(name)}: Components.${name}Calls =
         |  new ${name}CallsImpl();
         |"""
    }

    val componentCallImpls = services.map { case (name, service) =>
      val methods = service.commands
        .filter(_.isUnary)
        .map { command =>
          val commandMethod = lowerFirst(command.name)
          val inputType = fullyQualifiedMessage(command.inputType)
          val outputType = fullyQualifiedMessage(command.outputType)
          c"""override def $commandMethod(command: $inputType): $DeferredCall[$inputType, $outputType] =
             |  $ScalaDeferredCallAdapter(
             |    command,
             |    $Metadata.empty,
             |    "${service.fqn.fullyQualifiedProtoName}",
             |    "${command.name}",
             |    () => getGrpcClient(classOf[${service.fqn.fullyQualifiedGrpcServiceInterfaceName}]).$commandMethod(command)
             |  )"""
        }

      c"""private final class ${name}CallsImpl extends Components.${name}Calls {
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
         | $componentGetters
         |
         | $componentCallImpls
         |
         |}""")
  }

  private def nameFor(component: Service): String =
    component match {
      case as: ActionService => as.className.split('.').last
      case es: EntityService => es.componentFullName.split('.').last
      case vs: ViewService   => vs.className.split('.').last
    }

  private def fullyQualifiedMessage(messageType: FullyQualifiedName): String =
    s"${messageType.parent.javaPackage}.${messageType.name}"

}
