/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.codegen
package java

import kalix.codegen.ModelBuilder.EventSourcedEntity
import kalix.codegen.ModelBuilder.MessageTypeArgument
import kalix.codegen.ModelBuilder.ReplicatedEntity
import kalix.codegen.ModelBuilder.ValueEntity
import kalix.codegen.ModelBuilder.WorkflowComponent

/**
 * Responsible for generating Java source from an entity model
 */
object EntityServiceSourceGenerator {
  import kalix.codegen.SourceGeneratorUtils._
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
      entity: ModelBuilder.StatefulComponent,
      service: ModelBuilder.EntityService,
      mainClassPackageName: String,
      mainClassName: String,
      allServices: Seq[ModelBuilder.Service]): GeneratedFiles = {
    val entityPackage = entity.messageType.parent
    val servicePackage = service.messageType.parent
    val className = entity.messageType.name

    GeneratedFiles.Empty
      .addManaged(
        File.java(
          entityPackage,
          entity.abstractEntityName,
          interfaceSource(service, entity, entityPackage.javaPackage, className, mainClassPackageName)))
      .addManaged(File
        .java(entityPackage, entity.routerName, routerSource(service, entity, entityPackage.javaPackage, className)))
      .addManaged(
        File.java(
          entityPackage,
          entity.providerName,
          providerSource(service, entity, entityPackage.javaPackage, className, allServices)))
      .addUnmanaged(
        File.java(
          entityPackage,
          className,
          source(service, entity, entityPackage.javaPackage, className, entity.abstractEntityName)))
      .addIntegrationTest(File.java(
        servicePackage,
        className + "IntegrationTest",
        integrationTestSource(
          mainClassPackageName,
          mainClassName,
          service,
          entity,
          servicePackage.javaPackage,
          className + "IntegrationTest")))
  }

  private[codegen] def source(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.StatefulComponent,
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
      case valueEntity: WorkflowComponent =>
        WorkflowSourceGenerator.workflowSource(service, valueEntity, packageName, className)
    }
  }

  private[codegen] def interfaceSource(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.StatefulComponent,
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
          mainPackageName)
      case workflowComponent: WorkflowComponent =>
        WorkflowSourceGenerator.abstractWorkflowComponent(
          service,
          workflowComponent,
          packageName,
          className,
          mainPackageName)
    }

  private[codegen] def routerSource(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.StatefulComponent,
      packageName: String,
      className: String): String = {
    entity match {
      case entity: ModelBuilder.EventSourcedEntity =>
        EventSourcedEntitySourceGenerator.eventSourcedEntityRouter(service, entity, packageName, className)
      case entity: ValueEntity =>
        ValueEntitySourceGenerator.valueEntityRouter(service, entity, packageName, className)
      case entity: ReplicatedEntity =>
        ReplicatedEntitySourceGenerator.replicatedEntityRouter(service, entity, packageName, className)
      case entity: WorkflowComponent =>
        WorkflowSourceGenerator.workflowRouter(service, entity, packageName, className)
    }
  }

  private[codegen] def providerSource(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.StatefulComponent,
      packageName: String,
      className: String,
      allServices: Seq[ModelBuilder.Service]): String = {
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
      case workflowComponent: WorkflowComponent =>
        WorkflowSourceGenerator.workflowProvider(service, workflowComponent, packageName, className, allServices)
    }
  }

  private[codegen] def integrationTestSource(
      mainClassPackageName: String,
      mainClassName: String,
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.StatefulComponent,
      packageName: String,
      testClassName: String): String = {
    val serviceName = service.messageType.name

    val importTypes = commandTypes(service.commands) ++
      (entity match {
        case ModelBuilder.EventSourcedEntity(_, _, state, _) => Seq(state.messageType)
        case v: ModelBuilder.ValueEntity                     => Seq(v.state.messageType)
        case w: ModelBuilder.WorkflowComponent               => Seq(w.state.messageType)
        case ModelBuilder.ReplicatedEntity(_, _, data) =>
          data.typeArguments.collect { case MessageTypeArgument(messageType) => messageType }
      })

    val extraImports = entity match {
      case ModelBuilder.ReplicatedEntity(_, _, data) =>
        extraReplicatedImports(data) ++ extraTypeImports(data.typeArguments)
      case _ => Seq.empty
    }

    val imports = generateImports(
      importTypes,
      packageName,
      List(service.messageType.parent.javaPackage + "." + serviceName) ++
      Seq(
        "kalix.javasdk.testkit.junit.jupiter.KalixTestKitExtension",
        "org.junit.jupiter.api.extension.RegisterExtension",
        "org.junit.jupiter.api.Disabled",
        "org.junit.jupiter.api.Test",
        mainClassPackageName + "." + mainClassName) ++ extraImports)

    val testCases = service.commands.map { command =>
      s"""|@Test
          |@Disabled("to be implemented")
          |public void test${command.name}() throws Exception {
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
      |${unmanagedComment}
      |
      |// Example of an integration test calling our service via the Kalix Runtime
      |// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
      |public class $testClassName {
      |
      |  /**
      |   * The test kit starts both the service container and the Kalix Runtime.
      |   */
      |  @RegisterExtension
      |  public static final KalixTestKitExtension testKit =
      |    new KalixTestKitExtension(${mainClassName}.createKalix());
      |
      |  /**
      |   * Use the generated gRPC client to call the service through the Kalix Runtime.
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
