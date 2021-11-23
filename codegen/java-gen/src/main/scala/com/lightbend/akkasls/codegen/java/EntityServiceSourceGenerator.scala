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

package com.lightbend.akkasls.codegen
package java

import _root_.java.nio.file.Files
import _root_.java.nio.file.Path

import com.google.common.base.Charsets
import com.lightbend.akkasls.codegen.ModelBuilder.EventSourcedEntity
import com.lightbend.akkasls.codegen.ModelBuilder.MessageTypeArgument
import com.lightbend.akkasls.codegen.ModelBuilder.ReplicatedEntity
import com.lightbend.akkasls.codegen.ModelBuilder.ValueEntity

/**
 * Responsible for generating Java source from an entity model
 */
object EntityServiceSourceGenerator {
  import com.lightbend.akkasls.codegen.SourceGeneratorUtils._
  import JavaGeneratorUtils._

  /**
   * Generate Java source from entities where the target source and test source directories have no existing source.
   * Note that we only generate tests for entities where we are successful in generating an entity. The user may not
   * want a test otherwise.
   *
   * Also generates a main source file if it does not already exist.
   *
   * Impure.
   */
  def generate(
      entity: ModelBuilder.Entity,
      service: ModelBuilder.EntityService,
      sourceDirectory: Path,
      testSourceDirectory: Path,
      integrationTestSourceDirectory: Path,
      generatedSourceDirectory: Path,
      mainClassPackageName: String,
      mainClassName: String): Iterable[Path] = {
    val packageName = entity.fqn.parent.javaPackage
    val servicePackageName = service.fqn.parent.javaPackage
    val className = entity.fqn.name
    val packagePath = packageAsPath(packageName)
    val servicePackagePath = packageAsPath(servicePackageName)

    val implClassName = className
    val implSourcePath =
      sourceDirectory.resolve(packagePath.resolve(implClassName + ".java"))

    val interfaceClassName = entity.abstractEntityName
    val interfaceSourcePath =
      generatedSourceDirectory.resolve(packagePath.resolve(interfaceClassName + ".java"))

    interfaceSourcePath.getParent.toFile.mkdirs()
    Files.write(
      interfaceSourcePath,
      interfaceSource(service, entity, packageName, className, mainClassPackageName).getBytes(Charsets.UTF_8))

    if (!implSourcePath.toFile.exists()) {
      // Now we generate the entity
      implSourcePath.getParent.toFile.mkdirs()
      Files.write(
        implSourcePath,
        source(service, entity, packageName, implClassName, interfaceClassName).getBytes(Charsets.UTF_8))
    }

    val routerClassName = entity.routerName
    val routerSourcePath = {
      val path = generatedSourceDirectory.resolve(packagePath.resolve(routerClassName + ".java"))
      path.getParent.toFile.mkdirs()
      Files.write(path, routerSource(service, entity, packageName, className).getBytes(Charsets.UTF_8))
      path
    }

    val providerClassName = entity.providerName
    val providerSourcePath = {
      val path = generatedSourceDirectory.resolve(packagePath.resolve(providerClassName + ".java"))
      path.getParent.toFile.mkdirs()
      Files.write(path, providerSource(service, entity, packageName, className).getBytes(Charsets.UTF_8))
      path
    }

    // unit test
    val testClassName = className + "Test"
    val testSourcePath =
      testSourceDirectory.resolve(packagePath.resolve(testClassName + ".java"))
    val testSourceFiles = Nil // FIXME add new unit test generation

    // integration test
    val integrationTestClassName = className + "IntegrationTest"
    val integrationTestSourcePath =
      integrationTestSourceDirectory
        .resolve(servicePackagePath.resolve(integrationTestClassName + ".java"))
    if (!integrationTestSourcePath.toFile.exists()) {
      integrationTestSourcePath.getParent.toFile.mkdirs()
      Files.write(
        integrationTestSourcePath,
        integrationTestSource(
          mainClassPackageName,
          mainClassName,
          service,
          entity,
          servicePackageName,
          integrationTestClassName).getBytes(Charsets.UTF_8))
    }
    Seq(
      implSourcePath,
      integrationTestSourcePath,
      interfaceSourcePath,
      providerSourcePath,
      routerSourcePath) ++ testSourceFiles
  }

  private[codegen] def source(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.Entity,
      packageName: String,
      className: String,
      interfaceClassName: String): String = {
    entity match {
      case eventSourcedEntity: EventSourcedEntity =>
        EventSourcedEntitySourceGenerator.eventSourcedEntitySource(
          service,
          eventSourcedEntity,
          packageName,
          className,
          interfaceClassName)
      case valueEntity: ValueEntity =>
        ValueEntitySourceGenerator.valueEntitySource(service, valueEntity, packageName, className)
      case replicatedEntity: ReplicatedEntity =>
        ReplicatedEntitySourceGenerator.replicatedEntitySource(service, replicatedEntity, packageName, className)
    }
  }

  private[codegen] def interfaceSource(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.Entity,
      packageName: String,
      className: String,
      mainPackageName: String): String =
    entity match {
      case eventSourcedEntity: ModelBuilder.EventSourcedEntity =>
        EventSourcedEntitySourceGenerator.abstractEventSourcedEntity(
          service,
          eventSourcedEntity,
          packageName,
          className,
          mainPackageName)
      case valueEntity: ModelBuilder.ValueEntity =>
        ValueEntitySourceGenerator.abstractValueEntity(service, valueEntity, packageName, className, mainPackageName)
      case replicatedEntity: ReplicatedEntity =>
        ReplicatedEntitySourceGenerator.abstractReplicatedEntity(
          service,
          replicatedEntity,
          packageName,
          className,
          mainPackageName)
    }

  private[codegen] def routerSource(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.Entity,
      packageName: String,
      className: String): String = {
    entity match {
      case entity: ModelBuilder.EventSourcedEntity =>
        EventSourcedEntitySourceGenerator.eventSourcedEntityRouter(service, entity, packageName, className)
      case entity: ValueEntity =>
        ValueEntitySourceGenerator.valueEntityRouter(service, entity, packageName, className)
      case entity: ReplicatedEntity =>
        ReplicatedEntitySourceGenerator.replicatedEntityRouter(service, entity, packageName, className)
    }
  }

  private[codegen] def providerSource(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.Entity,
      packageName: String,
      className: String): String = {
    entity match {
      case eventSourcedEntity: ModelBuilder.EventSourcedEntity =>
        EventSourcedEntitySourceGenerator.eventSourcedEntityProvider(
          service,
          eventSourcedEntity,
          packageName,
          className)
      case valueEntity: ValueEntity =>
        ValueEntitySourceGenerator.valueEntityProvider(service, valueEntity, packageName, className)
      case replicatedEntity: ReplicatedEntity =>
        ReplicatedEntitySourceGenerator.replicatedEntityProvider(service, replicatedEntity, packageName, className)
    }
  }

  private[codegen] def integrationTestSource(
      mainClassPackageName: String,
      mainClassName: String,
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.Entity,
      packageName: String,
      testClassName: String): String = {
    val serviceName = service.fqn.name

    val importTypes = commandTypes(service.commands) ++
      (entity match {
        case ModelBuilder.EventSourcedEntity(_, _, state, _) => Seq(state.fqn)
        case v: ModelBuilder.ValueEntity                     => Seq(v.state.fqn)
        case ModelBuilder.ReplicatedEntity(_, _, data) =>
          data.typeArguments.collect { case MessageTypeArgument(fqn) => fqn }
      })

    val extraImports = entity match {
      case ModelBuilder.ReplicatedEntity(_, _, data) =>
        extraReplicatedImports(data) ++ extraTypeImports(data.typeArguments)
      case _ => Seq.empty
    }

    val imports = generateImports(
      importTypes,
      packageName,
      List(service.fqn.parent.javaPackage + "." + serviceName) ++
      Seq(
        "com.akkaserverless.javasdk.testkit.junit.AkkaServerlessTestKitResource",
        "org.junit.ClassRule",
        "org.junit.Test",
        mainClassPackageName + "." + mainClassName) ++ extraImports)

    val testCases = service.commands.map { command =>
      s"""|@Test
          |public void ${lowerFirst(command.name)}OnNonExistingEntity() throws Exception {
          |  // TODO: set fields in command, and provide assertions to match replies
          |  // client.${lowerFirst(command.name)}(${qualifiedType(command.inputType)}.newBuilder().build())
          |  //         .toCompletableFuture().get(5, SECONDS);
          |}
          |""".stripMargin

    }

    s"""package $packageName;
      |
      |${writeImports(imports)}
      |
      |import static java.util.concurrent.TimeUnit.*;
      |
      |$unmanagedComment
      |
      |// Example of an integration test calling our service via the Akka Serverless proxy
      |// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
      |public class $testClassName {
      |
      |  /**
      |   * The test kit starts both the service container and the Akka Serverless proxy.
      |   */
      |  @ClassRule
      |  public static final AkkaServerlessTestKitResource testKit =
      |    new AkkaServerlessTestKitResource(${mainClassName}.createAkkaServerless());
      |
      |  /**
      |   * Use the generated gRPC client to call the service through the Akka Serverless proxy.
      |   */
      |  private final $serviceName client;
      |
      |  public $testClassName() {
      |    client = testKit.getGrpcClient($serviceName.class);
      |  }
      |
      |  ${Format.indent(testCases, num = 2)}
      |}
      |""".stripMargin
  }

}
