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

package com.lightbend.akkasls.codegen.java

import com.google.common.base.Charsets
import com.lightbend.akkasls.codegen.Format
import com.lightbend.akkasls.codegen.ModelBuilder.ActionService
import com.lightbend.akkasls.codegen.ModelBuilder.EntityService
import com.lightbend.akkasls.codegen.ModelBuilder.Service
import com.lightbend.akkasls.codegen.ModelBuilder.ViewService

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * Generates convenience accessors for other components in the same service, accessible from actions
 */
object ComponentsSourceGenerator {
  import JavaGeneratorUtils._
  import com.lightbend.akkasls.codegen.SourceGeneratorUtils._

  def generate(
      generatedSourceDirectory: Path,
      packageName: String,
      serviceMap: Map[String, Service]): Iterable[Path] = {

    // since we want to flatten component names to as short as possible there may be duplicate
    // names, so for those we need to use a longer name
    val services = serviceMap.values.toSeq
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

    val packagePath = packageAsPath(packageName)
    val componentsFile = generatedSourceDirectory.resolve(packagePath.resolve("Components.java"))
    componentsFile.getParent.toFile.mkdirs()
    Files.write(
      componentsFile,
      generateComponentsInterface(packageName, uniqueNamesAndComponents).getBytes(Charsets.UTF_8))

    val componentsImplFile = generatedSourceDirectory.resolve(packagePath.resolve("ComponentsImpl.java"))
    componentsImplFile.getParent.toFile.mkdirs()
    Files.write(
      componentsImplFile,
      generateComponentsImpl(packageName, uniqueNamesAndComponents).getBytes(Charsets.UTF_8))

    componentsFile :: componentsImplFile :: Nil
  }

  private def generateComponentsInterface(packageName: String, services: Map[String, Service]): String = {
    val imports = generateImports(Nil, packageName, otherImports = Seq("com.akkaserverless.javasdk.DeferredCall"))

    val componentCalls = services.map { case (name, component) =>
      // FIXME only unary deferred calls supported for now, or could streamed out be supported if types align?
      // higher risk of name conflicts here where all components in the service meets up, so all
      // type names are fully qualified rather than imported
      val unaryCommands = component.commands.filter(_.isUnary)
      val methods = unaryCommands
        .map(command =>
          s"""DeferredCall<${command.inputType.fullyQualifiedJavaName}, ${command.outputType.fullyQualifiedJavaName}> ${lowerFirst(
            command.name)}(${command.inputType.fullyQualifiedJavaName} ${lowerFirst(command.inputType.name)});
             |""".stripMargin)

      // FIXME component name might not be unique across service?
      s"""interface ${name}Calls {
         |  ${Format.indent(methods, 2)}
         |}""".stripMargin
    }

    val componentGetters = services.map { case (name, _) =>
      s"${name}Calls ${lowerFirst(name)}();"
    }

    s"""package $packageName;
      |
      |${writeImports(imports)}
      |
      |$managedComment
      |
      |public interface Components {
      |  ${Format.indent(componentGetters, 2)}
      |
      |  ${Format.indent(componentCalls, 2)}
      |}
      |""".stripMargin
  }

  private def generateComponentsImpl(packageName: String, services: Map[String, Service]): String = {
    val imports = generateImports(
      Nil,
      packageName,
      otherImports = Seq(
        "com.akkaserverless.javasdk.DeferredCall",
        "com.akkaserverless.javasdk.Context",
        "com.akkaserverless.javasdk.impl.DeferredCallImpl",
        "com.akkaserverless.javasdk.impl.MetadataImpl",
        "com.akkaserverless.javasdk.impl.AbstractContext"))

    val componentGetters = services.map { case (name, _) =>
      s"""@Override
         |public Components.${name}Calls ${lowerFirst(name)}() {
         |  return new ${name}CallsImpl();
         |}""".stripMargin
    }

    val componentCallImpls = services.map { case (name, service) =>
      val methods = service.commands
        .filter(_.isUnary)
        .map { command =>
          val commandMethod = lowerFirst(command.name)
          val paramName = lowerFirst(command.inputType.name)
          s"""@Override
             |public DeferredCall<${command.inputType.fullyQualifiedJavaName}, ${command.outputType.fullyQualifiedJavaName}> $commandMethod(${command.inputType.fullyQualifiedJavaName} $paramName) {
             |  return new DeferredCallImpl<>(
             |    ${lowerFirst(command.inputType.name)},
             |    MetadataImpl.Empty(),
             |    "${service.fqn.fullyQualifiedProtoName}",
             |    "${command.name}",
             |    () -> getGrpcClient(${service.fqn.fullyQualifiedGrpcServiceInterfaceName}.class).$commandMethod($paramName)
             |  );
             |}""".stripMargin
        }

      s"""private final class ${name}CallsImpl implements Components.${name}Calls {
         |   ${Format.indent(methods, 2)}
         |}""".stripMargin

    }

    s"""package $packageName;
       |
       |${writeImports(imports)}
       |
       |$managedComment
       |
       |/**
       | * Not intended for direct instantiation, called by generated code, use Action.components() to access
       | */
       |public final class ComponentsImpl implements Components {
       |
       |  private final AbstractContext context;
       |
       |  public ComponentsImpl(Context context) {
       |    this.context = (AbstractContext) context;
       |  }
       |
       |  private <T> T getGrpcClient(Class<T> serviceClass) {
       |    return context.getComponentGrpcClient(serviceClass);
       |  }
       |
       |  ${Format.indent(componentGetters, 2)}
       |
       |  ${Format.indent(componentCallImpls, 2)}
       |}
       |""".stripMargin
  }

  private def nameFor(component: Service): String = {
    component match {
      case as: ActionService => as.className.split('.').last
      case es: EntityService => es.componentFullName.split('.').last
      case vs: ViewService   => vs.className.split('.').last
    }
  }
}
