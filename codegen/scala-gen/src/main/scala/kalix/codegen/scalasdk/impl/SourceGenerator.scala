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

import kalix.codegen.{ File, GeneratedFiles, ModelBuilder, PackageNaming, SourceGeneratorUtils }

object SourceGenerator {

  /**
   * Generate the 'managed' code for this model: code that will be regenerated regularly in the 'compile' configuratio
   */
  def generateManaged(model: ModelBuilder.Model, configuredRootPackage: Option[String]): Seq[File] = {
    val mainPackageName = configuredRootPackage
      .map(nameForMainPackage)
      .getOrElse(nameForMainPackage(model))

    if (model.services.values.isEmpty) {
      throw new IllegalStateException(
        "Project does not contain any gRPC service descriptors annotated as Kalix components with 'option (kalix.codegen)'. " +
        "For details on declaring services see documentation: https://docs.kalix.io/java/writing-grpc-descriptors-protobuf.html#_service")
    }
    MainSourceGenerator.generateManaged(model, mainPackageName).toSeq ++
    ComponentSourceGenerator.generateManaged(model, mainPackageName) ++
    model.services.values
      .flatMap {
        case service: ModelBuilder.EntityService =>
          model.lookupEntity(service) match {
            case entity: ModelBuilder.ValueEntity =>
              ValueEntitySourceGenerator.generateManaged(entity, service, mainPackageName)
            case entity: ModelBuilder.EventSourcedEntity =>
              EventSourcedEntitySourceGenerator.generateManaged(entity, service, mainPackageName)
            case entity: ModelBuilder.ReplicatedEntity =>
              ReplicatedEntitySourceGenerator.generateManaged(entity, service)
            case workflow: ModelBuilder.WorkflowComponent =>
              WorkflowSourceGenerator.generateManaged(workflow, service, mainPackageName, model.services.values.toSeq)
          }
        case service: ModelBuilder.ViewService =>
          ViewServiceSourceGenerator.generateManaged(service)
        case service: ModelBuilder.ActionService =>
          ActionServiceSourceGenerator.generateManaged(service, mainPackageName)
      }
  }

  /**
   * Generate the 'managed' code for this model: code that will be regenerated regularly in the 'compile' configuratio
   */
  def generateManagedTest(model: ModelBuilder.Model): Seq[File] = {
    model.services.values.flatMap {
      case service: ModelBuilder.EntityService =>
        model.lookupEntity(service) match {
          case entity: ModelBuilder.ValueEntity =>
            ValueEntityTestKitGenerator.generateManagedTest(entity, service)
          case entity: ModelBuilder.EventSourcedEntity =>
            EventSourcedEntityTestKitGenerator.generateManagedTest(entity, service)
          case _: ModelBuilder.ReplicatedEntity =>
            Nil
          case _: ModelBuilder.WorkflowComponent =>
            Nil
        }
      case _: ModelBuilder.ViewService =>
        Nil
      case service: ModelBuilder.ActionService =>
        ActionTestKitGenerator.generateManagedTest(service)
    }.toList
  }

  /**
   * Generate the 'unmanaged' code for this model: code that is generated once on demand and then maintained by the
   * user.
   */
  def generateUnmanaged(model: ModelBuilder.Model, configuredRootPackage: Option[String]): Seq[File] = {
    val mainPackageName = configuredRootPackage
      .map(nameForMainPackage)
      .getOrElse(nameForMainPackage(model))

    MainSourceGenerator.generateUnmanaged(model, mainPackageName).toSeq ++
    model.services.values
      .flatMap {
        case service: ModelBuilder.EntityService =>
          model.lookupEntity(service) match {
            case entity: ModelBuilder.ValueEntity =>
              ValueEntitySourceGenerator.generateUnmanaged(entity, service)
            case entity: ModelBuilder.EventSourcedEntity =>
              EventSourcedEntitySourceGenerator.generateUnmanaged(entity, service)
            case entity: ModelBuilder.ReplicatedEntity =>
              ReplicatedEntitySourceGenerator.generateUnmanaged(entity, service)
            case workflow: ModelBuilder.WorkflowComponent =>
              WorkflowSourceGenerator.generateUnmanaged(workflow, service)
          }
        case service: ModelBuilder.ViewService =>
          ViewServiceSourceGenerator.generateUnmanaged(service)
        case service: ModelBuilder.ActionService =>
          ActionServiceSourceGenerator.generateUnmanaged(service)
      }
  }

  /**
   * Generate the 'unmanaged' code for this model: code that is generated once on demand and then maintained by the user
   */
  def generateUnmanagedTest(model: ModelBuilder.Model, configuredRootPackage: Option[String]): Seq[File] = {
    val mainPackageName = configuredRootPackage
      .map(nameForMainPackage)
      .getOrElse(nameForMainPackage(model))

    model.services.values.flatMap {
      case service: ModelBuilder.EntityService =>
        val main = MainSourceGenerator.mainClassName(model, mainPackageName)
        model.lookupEntity(service) match {
          case entity: ModelBuilder.ValueEntity =>
            ValueEntityTestKitGenerator.generateUnmanagedTest(main, entity, service)
          case entity: ModelBuilder.EventSourcedEntity =>
            EventSourcedEntityTestKitGenerator.generateUnmanagedTest(main, entity, service)
          case _: ModelBuilder.ReplicatedEntity =>
            Nil
          case _: ModelBuilder.WorkflowComponent =>
            Nil
        }
      case _: ModelBuilder.ViewService =>
        Nil
      case service: ModelBuilder.ActionService =>
        ActionTestKitGenerator.generateUnmanagedTest(service)
    }.toList
  }

  /**
   * Genereate files to a `GeneratedFiles` structure similar to what Java does for testing purposes.
   */
  def generateFiles(model: ModelBuilder.Model, configuredRootPackage: Option[String]): GeneratedFiles =
    GeneratedFiles(
      managedFiles = generateManaged(model, configuredRootPackage),
      unmanagedFiles = generateUnmanaged(model, configuredRootPackage),
      managedTestFiles = generateManagedTest(model),
      unmanagedTestFiles = generateUnmanagedTest(model, configuredRootPackage),
      integrationTestFiles = Nil)

  private def nameForMainPackage(packageString: String): PackageNaming =
    new PackageNaming(
      protoFileName = "",
      name = "",
      protoPackage = packageString,
      javaPackageOption = Some(packageString),
      javaOuterClassnameOption = None,
      javaMultipleFiles = false)

  private def nameForMainPackage(model: ModelBuilder.Model): PackageNaming = {
    val services = model.services.values.map(_.messageType.fullyQualifiedName)
    val entities = model.statefulComponents.values.map(_.messageType.fullyQualifiedName)
    nameForMainPackage(SourceGeneratorUtils.mainPackageName(services ++ entities).mkString("."))
  }
}
