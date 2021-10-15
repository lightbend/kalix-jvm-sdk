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
import com.lightbend.akkasls.codegen.ModelBuilder.Entity
import com.lightbend.akkasls.codegen.ModelBuilder.Service
import com.lightbend.akkasls.codegen._

/**
 * Responsible for generating Main and AkkaServerlessFactory Java source from an entity model
 */
object MainSourceGenerator {

  import com.lightbend.akkasls.codegen.SourceGeneratorUtils._
  import ScalaGeneratorUtils._

  def generateUnmanaged(model: ModelBuilder.Model): Iterable[File] =
    Seq(mainSource(model))

  def generateManaged(model: ModelBuilder.Model): Iterable[File] =
    Seq(akkaServerlessFactorySource(model))

  def mainClassName(model: ModelBuilder.Model): FullyQualifiedName = {
    val packageName = mainPackageName(model.services.keys ++ model.entities.keys).mkString(".")
    val className = "Main"
    FullyQualifiedName.noDescriptor(
      className,
      new PackageNaming(
        protoFileName = "",
        name = "",
        protoPackage = packageName,
        javaPackageOption = None,
        javaOuterClassnameOption = None,
        javaMultipleFiles = false))
  }

  private[codegen] def mainSource(model: ModelBuilder.Model): File = {
    val mainClass = mainClassName(model)

    val entityImports = model.entities.values.collect {
      case entity: ModelBuilder.EventSourcedEntity => entity.fqn.fullyQualifiedJavaName
      case entity: ModelBuilder.ValueEntity        => entity.fqn.fullyQualifiedJavaName
      case entity: ModelBuilder.ReplicatedEntity   => entity.fqn.fullyQualifiedJavaName
    }.toSeq

    val serviceImports = model.services.values.collect {
      case service: ModelBuilder.ActionService => service.classNameQualified
      case view: ModelBuilder.ViewService      => view.classNameQualified
    }.toSeq

    val allImports = entityImports ++ serviceImports ++
      List("com.akkaserverless.scalasdk.AkkaServerless", "org.slf4j.LoggerFactory")

    implicit val imports: Imports =
      generateImports(Iterable.empty, mainClass.parent.scalaPackage, allImports)

    val entityRegistrationParameters = model.entities.values.toList
      .sortBy(_.fqn.name)
      .collect {
        case entity: ModelBuilder.EventSourcedEntity => s"new ${typeName(entity.fqn)}(_)"
        case entity: ModelBuilder.ValueEntity        => s"new ${typeName(entity.fqn)}(_)"
        case entity: ModelBuilder.ReplicatedEntity   => s"new ${typeName(entity.fqn)}(_)"
      }

    val serviceRegistrationParameters = model.services.values.toList
      .sortBy(_.fqn.name)
      .collect {
        case service: ModelBuilder.ActionService => s"new ${service.className}(_)"
        case view: ModelBuilder.ViewService      => s"new ${view.className}(_)"
      }

    val registrationParameters = entityRegistrationParameters ::: serviceRegistrationParameters

    File(
      mainClass.parent.scalaPackage,
      mainClass.name,
      s"""|package ${mainClass.parent.scalaPackage}
        |
        |${writeImports(imports)}
        |
        |$unmanagedComment
        |
        |object ${mainClass.name} {
        |
        |  private val log = LoggerFactory.getLogger("${mainClass.parent.scalaPackage}.${mainClass.name}")
        |
        |  def createAkkaServerless(): AkkaServerless = {
        |    // The AkkaServerlessFactory automatically registers any generated Actions, Views or Entities,
        |    // and is kept up-to-date with any changes in your protobuf definitions.
        |    // If you prefer, you may remove this and manually register these components in a
        |    // `AkkaServerless()` instance.
        |    AkkaServerlessFactory.withComponents(
        |      ${registrationParameters.mkString(",\n      ")})
        |  }
        |
        |  def main(args: Array[String]): Unit = {
        |    log.info("starting the Akka Serverless service")
        |    createAkkaServerless().start()
        |  }
        |}
        |""".stripMargin)
  }

  private[codegen] def akkaServerlessFactorySource(model: ModelBuilder.Model): File = {
    val packageName = mainPackageName(model.services.keys ++ model.entities.keys).mkString(".")

    val entityImports = model.entities.values.flatMap { ety =>
      val imp =
        ety.fqn :: Nil
      ety match {
        case _: ModelBuilder.EventSourcedEntity =>
          ety.provider :: imp
        case _: ModelBuilder.ValueEntity =>
          ety.provider :: imp
        case _: ModelBuilder.ReplicatedEntity =>
          ety.provider :: imp
        case _ => imp
      }
    }

    val serviceImports = model.services.values.flatMap { serv =>
      serv match {
        case actionServ: ModelBuilder.ActionService =>
          List(actionServ.classNameQualified, actionServ.providerNameQualified)
        case view: ModelBuilder.ViewService =>
          List(view.classNameQualified, view.providerNameQualified)
        case _ => Nil
      }
    }

    val entityContextImports = model.entities.values.collect {
      case _: ModelBuilder.EventSourcedEntity =>
        List("com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityContext")
      case _: ModelBuilder.ValueEntity =>
        List("com.akkaserverless.scalasdk.valueentity.ValueEntityContext")
      case _: ModelBuilder.ReplicatedEntity =>
        List("com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityContext")
    }.flatten

    val serviceContextImports = model.services.values.collect {
      case _: ModelBuilder.ActionService =>
        List("com.akkaserverless.scalasdk.action.ActionCreationContext")
      case _: ModelBuilder.ViewService =>
        List("com.akkaserverless.scalasdk.view.ViewCreationContext")
    }.flatten

    implicit val imports: Imports =
      generateImports(
        entityImports,
        packageName,
        Seq("com.akkaserverless.scalasdk.AkkaServerless") ++ serviceImports ++ entityContextImports ++ serviceContextImports)

    def creator(fqn: FullyQualifiedName): String = {
      if (imports.clashingNames.contains(fqn.name)) s"create${dotsToCamelCase(typeName(fqn))}"
      else s"create${fqn.name}"
    }

    val registrations = model.services.values
      .flatMap {
        case service: ModelBuilder.EntityService =>
          model.entities.get(service.componentFullName).toSeq.map {
            case entity: ModelBuilder.EventSourcedEntity =>
              s".register(${typeName(entity.provider)}(${creator(entity.fqn)}))"
            case entity: ModelBuilder.ValueEntity =>
              s".register(${typeName(entity.provider)}(${creator(entity.fqn)}))"
            case entity: ModelBuilder.ReplicatedEntity =>
              s".register(${typeName(entity.provider)}(${creator(entity.fqn)}))"
          }

        case service: ModelBuilder.ViewService =>
          List(s".register(${service.providerName}(${creator(service.impl)}))")

        case service: ModelBuilder.ActionService =>
          List(s".register(${service.providerName}(${creator(service.impl)}))")

      }
      .toList
      .sorted

    val entityCreators =
      model.entities.values.toList
        .sortBy(_.fqn.name)
        .collect {
          case entity: ModelBuilder.EventSourcedEntity =>
            s"${creator(entity.fqn)}: EventSourcedEntityContext => ${typeName(entity.fqn)}"
          case entity: ModelBuilder.ValueEntity =>
            s"${creator(entity.fqn)}: ValueEntityContext => ${typeName(entity.fqn)}"
          case entity: ModelBuilder.ReplicatedEntity =>
            s"${creator(entity.fqn)}: ReplicatedEntityContext => ${typeName(entity.fqn)}"
        }

    val serviceCreators = model.services.values.toList
      .sortBy(_.fqn.name)
      .collect {
        case service: ModelBuilder.ActionService =>
          s"${creator(service.impl)}: ActionCreationContext => ${typeName(service.impl)}"
        case view: ModelBuilder.ViewService =>
          s"${creator(view.impl)}: ViewCreationContext => ${typeName(view.impl)}"
      }

    val creatorParameters = entityCreators ::: serviceCreators

    File(
      packageName,
      "AkkaServerlessFactory",
      s"""|package $packageName
        |
        |${writeImports(imports)}
        |
        |$managedComment
        |
        |object AkkaServerlessFactory {
        |
        |  def withComponents(
        |      ${creatorParameters.mkString(",\n      ")}): AkkaServerless = {
        |    val akkaServerless = AkkaServerless()
        |    akkaServerless
        |      ${Format.indent(registrations, 6)}
        |  }
        |}
        |""".stripMargin)
  }

}
