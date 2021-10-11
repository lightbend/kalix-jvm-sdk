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

import _root_.java.io.File
import _root_.java.nio.file.Files
import _root_.java.nio.file.Path

import scala.collection.immutable

import com.google.common.base.Charsets
import com.lightbend.akkasls.codegen.DescriptorSet
import com.lightbend.akkasls.codegen.Log
import com.lightbend.akkasls.codegen.ModelBuilder
import com.lightbend.akkasls.codegen.ModelBuilder.Entity
import com.lightbend.akkasls.codegen.ModelBuilder.Service
import com.lightbend.akkasls.codegen._
import sun.tools.java.Imports

/**
 * Responsible for generating Main and AkkaServerlessFactory Java source from an entity model
 */
object MainSourceGenerator {
  import com.lightbend.akkasls.codegen.SourceGeneratorUtils._

  implicit val lang = Java

  def generate(
      model: ModelBuilder.Model,
      mainClassPackageName: String,
      mainClassName: String,
      sourceDirectory: Path,
      generatedSourceDirectory: Path): Iterable[Path] = {

    val mainClassPackagePath = packageAsPath(mainClassPackageName)

    val akkaServerlessFactorySourcePath =
      generatedSourceDirectory.resolve(mainClassPackagePath.resolve("AkkaServerlessFactory.java"))

    akkaServerlessFactorySourcePath.getParent.toFile.mkdirs()
    Files.write(
      akkaServerlessFactorySourcePath,
      akkaServerlessFactorySource(mainClassPackageName, model).getBytes(Charsets.UTF_8))

    // Generate a main source file if it is not there already

    val mainClassPath =
      sourceDirectory.resolve(mainClassPackagePath.resolve(mainClassName + ".java"))
    if (!mainClassPath.toFile.exists()) {
      mainClassPath.getParent.toFile.mkdirs()
      Files.write(
        mainClassPath,
        mainSource(mainClassPackageName, mainClassName, model.entities, model.services).getBytes(Charsets.UTF_8))
    }
    List(akkaServerlessFactorySourcePath, mainClassPath)
  }

  private[codegen] def mainSource(
      mainClassPackageName: String,
      mainClassName: String,
      entities: Map[String, Entity],
      services: Map[String, Service]): String = {

    val entityImports = entities.values.collect {
      case entity: ModelBuilder.EventSourcedEntity => entity.fqn.fullQualifiedName
      case entity: ModelBuilder.ValueEntity        => entity.fqn.fullQualifiedName
      case entity: ModelBuilder.ReplicatedEntity   => entity.fqn.fullQualifiedName
    }.toSeq

    val serviceImports = services.values.collect {
      case service: ModelBuilder.ActionService => service.classNameQualified
      case view: ModelBuilder.ViewService      => view.classNameQualified
    }.toSeq

    val componentImports = generateImports(Iterable.empty, mainClassPackageName, entityImports ++ serviceImports)
    val entityRegistrationParameters = entities.values.toList
      .sortBy(_.fqn.name)
      .collect {
        case entity: ModelBuilder.EventSourcedEntity => s"${entity.fqn.name}::new"
        case entity: ModelBuilder.ValueEntity        => s"${entity.fqn.name}::new"
        case entity: ModelBuilder.ReplicatedEntity   => s"${entity.fqn.name}::new"
      }

    val serviceRegistrationParameters = services.values.toList
      .sortBy(_.fqn.name)
      .collect {
        case service: ModelBuilder.ActionService => s"${service.className}::new"
        case view: ModelBuilder.ViewService      => s"${view.className}::new"
      }

    val registrationParameters = entityRegistrationParameters ::: serviceRegistrationParameters
    s"""package $mainClassPackageName;
        |
        |import com.akkaserverless.javasdk.AkkaServerless;
        |import org.slf4j.Logger;
        |import org.slf4j.LoggerFactory;
        |${lang.writeImports(componentImports)}
        |
        |$unmanagedComment
        |
        |public final class ${mainClassName} {
        |
        |  private static final Logger LOG = LoggerFactory.getLogger(${mainClassName}.class);
        |
        |  public static AkkaServerless createAkkaServerless() {
        |    // The AkkaServerlessFactory automatically registers any generated Actions, Views or Entities,
        |    // and is kept up-to-date with any changes in your protobuf definitions.
        |    // If you prefer, you may remove this and manually register these components in a
        |    // `new AkkaServerless()` instance.
        |    return AkkaServerlessFactory.withComponents(
        |      ${registrationParameters.mkString(",\n      ")});
        |  }
        |
        |  public static void main(String[] args) throws Exception {
        |    LOG.info("starting the Akka Serverless service");
        |    createAkkaServerless().start();
        |  }
        |}
        |""".stripMargin

  }

  private[codegen] def akkaServerlessFactorySource(mainClassPackageName: String, model: ModelBuilder.Model): String = {
    val registrations = model.services.values
      .flatMap {
        case service: ModelBuilder.EntityService =>
          model.entities.get(service.componentFullName).toSeq.map {
            case entity: ModelBuilder.EventSourcedEntity =>
              s".register(${entity.fqn.name}Provider.of(create${entity.fqn.name}))"
            case entity: ModelBuilder.ValueEntity =>
              s".register(${entity.fqn.name}Provider.of(create${entity.fqn.name}))"
            case entity: ModelBuilder.ReplicatedEntity =>
              s".register(${entity.fqn.name}Provider.of(create${entity.fqn.name}))"
          }

        case service: ModelBuilder.ViewService =>
          List(s".register(${service.providerName}.of(create${service.className}))")

        case service: ModelBuilder.ActionService =>
          List(s".register(${service.providerName}.of(create${service.className}))")

      }
      .toList
      .sorted

    val entityImports = model.entities.values.flatMap { ety =>
      if (ety.fqn.parent.javaPackage != mainClassPackageName) {
        val imports =
          ety.fqn.fullQualifiedName ::
          s"${ety.fqn.parent.javaPackage}.${ety.fqn.parent.javaOuterClassname}" ::
          Nil
        ety match {
          case _: ModelBuilder.EventSourcedEntity =>
            s"${ety.fqn.fullQualifiedName}Provider" :: imports
          case _: ModelBuilder.ValueEntity =>
            s"${ety.fqn.fullQualifiedName}Provider" :: imports
          case _: ModelBuilder.ReplicatedEntity =>
            s"${ety.fqn.fullQualifiedName}Provider" :: imports
          case _ => imports
        }
      } else List.empty
    }

    val serviceImports = model.services.values.flatMap { serv =>
      if (serv.fqn.parent.javaPackage != mainClassPackageName) {
        val outerClass = s"${serv.fqn.parent.javaPackage}.${serv.fqn.parent.javaOuterClassname}"
        serv match {
          case actionServ: ModelBuilder.ActionService =>
            List(actionServ.classNameQualified, actionServ.providerNameQualified, outerClass)
          case view: ModelBuilder.ViewService =>
            List(view.classNameQualified, view.providerNameQualified, outerClass)
          case _ => List(outerClass)
        }
      } else List.empty
    }

    val otherImports = model.services.values.flatMap { serv =>
      val types = serv.commands.flatMap { cmd =>
        cmd.inputType :: cmd.outputType :: Nil
      }
      collectRelevantTypes(types, serv.fqn).map { typ =>
        s"${typ.parent.javaPackage}.${typ.parent.javaOuterClassname}"
      }
    }

    val entityContextImports = model.entities.values.collect {
      case _: ModelBuilder.EventSourcedEntity =>
        List("com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityContext", "java.util.function.Function")
      case _: ModelBuilder.ValueEntity =>
        List("com.akkaserverless.javasdk.valueentity.ValueEntityContext", "java.util.function.Function")
      case _: ModelBuilder.ReplicatedEntity =>
        List("com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext", "java.util.function.Function")
    }.flatten

    val serviceContextImports = model.services.values.collect {
      case _: ModelBuilder.ActionService =>
        List("com.akkaserverless.javasdk.action.ActionCreationContext", "java.util.function.Function")
      case _: ModelBuilder.ViewService =>
        List("com.akkaserverless.javasdk.view.ViewCreationContext", "java.util.function.Function")
    }.flatten
    val contextImports = (entityContextImports ++ serviceContextImports).toSet

    val entityCreators =
      model.entities.values.toList
        .sortBy(_.fqn.name)
        .collect {
          case entity: ModelBuilder.EventSourcedEntity =>
            s"Function<EventSourcedEntityContext, ${entity.fqn.name}> create${entity.fqn.name}"
          case entity: ModelBuilder.ValueEntity =>
            s"Function<ValueEntityContext, ${entity.fqn.name}> create${entity.fqn.name}"
          case entity: ModelBuilder.ReplicatedEntity =>
            s"Function<ReplicatedEntityContext, ${entity.fqn.name}> create${entity.fqn.name}"
        }

    val serviceCreators = model.services.values.toList
      .sortBy(_.fqn.name)
      .collect {
        case service: ModelBuilder.ActionService =>
          s"Function<ActionCreationContext, ${service.className}> create${service.className}"
        case view: ModelBuilder.ViewService =>
          s"Function<ViewCreationContext, ${view.className}> create${view.className}"
      }

    val creatorParameters = entityCreators ::: serviceCreators
    val imports =
      (List(
        "com.akkaserverless.javasdk.AkkaServerless") ++ entityImports ++ serviceImports ++ otherImports ++ contextImports).distinct.sorted
        .map(pkg => s"import $pkg;")
        .mkString("\n")

    s"""package $mainClassPackageName;
        |
        |$imports
        |
        |$managedComment
        |
        |public final class AkkaServerlessFactory {
        |
        |  public static AkkaServerless withComponents(
        |      ${creatorParameters.mkString(",\n      ")}) {
        |    AkkaServerless akkaServerless = new AkkaServerless();
        |    return akkaServerless
        |      ${Format.indent(registrations, 6)};
        |  }
        |}
        |""".stripMargin
  }

}
