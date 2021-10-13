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

import _root_.java.nio.file.Files
import _root_.java.nio.file.Path
import com.google.common.base.Charsets
import com.lightbend.akkasls.codegen.ModelBuilder
import com.lightbend.akkasls.codegen.ModelBuilder.Entity
import com.lightbend.akkasls.codegen.ModelBuilder.Service
import com.lightbend.akkasls.codegen._

import scala.annotation.tailrec

/**
 * Responsible for generating Main and AkkaServerlessFactory Java source from an entity model
 */
object MainSourceGenerator {
  import com.lightbend.akkasls.codegen.SourceGeneratorUtils._
  import JavaGeneratorUtils._

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
      case entity: ModelBuilder.EventSourcedEntity => entity.impl
      case entity: ModelBuilder.ValueEntity        => entity.impl
      case entity: ModelBuilder.ReplicatedEntity   => entity.impl
    }.toSeq

    val serviceImports = services.values.collect {
      case service: ModelBuilder.ActionService => service.impl
      case view: ModelBuilder.ViewService      => view.impl
    }.toSeq

    implicit val imports: Imports =
      generateImports(
        entityImports ++ serviceImports,
        mainClassPackageName,
        Seq("com.akkaserverless.javasdk.AkkaServerless", "org.slf4j.Logger", "org.slf4j.LoggerFactory"))

    val entityRegistrationParameters = entities.values.toList
      .sortBy(_.fqn.name)
      .collect {
        case entity: ModelBuilder.EventSourcedEntity => s"${typeName(entity.impl)}::new"
        case entity: ModelBuilder.ValueEntity        => s"${typeName(entity.impl)}::new"
        case entity: ModelBuilder.ReplicatedEntity   => s"${typeName(entity.impl)}::new"
      }

    val serviceRegistrationParameters = services.values.toList
      .sortBy(_.fqn.name)
      .collect {
        case service: ModelBuilder.ActionService => s"${typeName(service.impl)}::new"
        case view: ModelBuilder.ViewService      => s"${typeName(view.impl)}::new"
      }

    val registrationParameters = entityRegistrationParameters ::: serviceRegistrationParameters
    s"""package $mainClassPackageName;
        |
        |${writeImports(imports)}
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
    val entityImports = model.entities.values.flatMap { ety =>
      Seq(ety.impl, ety.provider)
    }

    val serviceImports = model.services.values.flatMap { serv =>
      serv.fqn.descriptorObject ++
      (serv match {
        case actionServ: ModelBuilder.ActionService =>
          List(actionServ.impl, actionServ.provider)
        case view: ModelBuilder.ViewService =>
          List(view.impl, view.provider)
        case _ => Nil
      })
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
    val contextImports = (entityContextImports ++ serviceContextImports).toSeq

    implicit val imports =
      generateImports(
        entityImports ++ serviceImports,
        mainClassPackageName,
        "com.akkaserverless.javasdk.AkkaServerless" +: contextImports)

    def creator(fqn: FullyQualifiedName): String = {
      if (imports.clashingNames.contains(fqn.name)) s"create${dotsToCamelCase(typeName(fqn))}"
      else s"create${fqn.name}"
    }

    val registrations = model.services.values
      .flatMap {
        case service: ModelBuilder.EntityService =>
          model.entities.get(service.componentFullName).toSeq.map {
            case entity: ModelBuilder.EventSourcedEntity =>
              s".register(${typeName(entity.provider)}.of(${creator(entity.impl)}))"
            case entity: ModelBuilder.ValueEntity =>
              s".register(${typeName(entity.provider)}.of(${creator(entity.impl)}))"
            case entity: ModelBuilder.ReplicatedEntity =>
              s".register(${typeName(entity.provider)}.of(${creator(entity.impl)}))"
          }

        case service: ModelBuilder.ViewService =>
          List(s".register(${typeName(service.provider)}.of(${creator(service.impl)}))")

        case service: ModelBuilder.ActionService =>
          List(s".register(${typeName(service.provider)}.of(${creator(service.impl)}))")

      }
      .toList
      .sorted

    val entityCreators =
      model.entities.values.toList
        .sortBy(_.fqn.name)
        .collect {
          case entity: ModelBuilder.EventSourcedEntity =>
            s"Function<EventSourcedEntityContext, ${typeName(entity.impl)}> ${creator(entity.impl)}"
          case entity: ModelBuilder.ValueEntity =>
            s"Function<ValueEntityContext, ${typeName(entity.impl)}> ${creator(entity.impl)}"
          case entity: ModelBuilder.ReplicatedEntity =>
            s"Function<ReplicatedEntityContext, ${typeName(entity.impl)}> ${creator(entity.impl)}"
        }

    val serviceCreators = model.services.values.toList
      .sortBy(_.fqn.name)
      .collect {
        case service: ModelBuilder.ActionService =>
          s"Function<ActionCreationContext, ${typeName(service.impl)}> ${creator(service.impl)}"
        case view: ModelBuilder.ViewService =>
          s"Function<ViewCreationContext, ${typeName(view.impl)}> ${creator(view.impl)}"
      }

    val creatorParameters = entityCreators ::: serviceCreators

    s"""package $mainClassPackageName;
        |
        |${writeImports(imports)}
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
