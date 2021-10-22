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

  def generate(generatedSourceDirectory: Path, packageName: String, services: Map[String, Service]): Iterable[Path] = {
    val packagePath = packageAsPath(packageName)
    val componentsFile = generatedSourceDirectory.resolve(packagePath.resolve("Components.java"))
    componentsFile.getParent.toFile.mkdirs()
    Files.write(
      componentsFile,
      generateComponentsInterface(packageName, services.values.toSeq).getBytes(Charsets.UTF_8))

    componentsFile :: Nil
  }

  private def generateComponentsInterface(packageName: String, services: Seq[Service]): String = {
    val imports = generateImports(Nil, packageName, otherImports = Seq("com.akkaserverless.javasdk.DeferredCall"))

    // since we want to flatten component names to as short as possible there may be duplicate
    // names, so for those we need to use a longer name
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

    val componentCalls = uniqueNamesAndComponents.map { case (name, component) =>
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

    val componentGetters = uniqueNamesAndComponents.map { case (name, _) =>
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

  private def nameFor(component: Service): String = {
    component match {
      case as: ActionService => as.className.split('.').last
      case es: EntityService => es.componentFullName.split('.').last
      case vs: ViewService   => vs.className.split('.').last
    }
  }
}
