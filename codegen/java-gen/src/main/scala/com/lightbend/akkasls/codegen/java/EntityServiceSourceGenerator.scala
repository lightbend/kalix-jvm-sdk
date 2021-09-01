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

import com.google.common.base.Charsets
import scala.collection.immutable
import _root_.java.nio.file.{Files, Path}

import com.lightbend.akkasls.codegen.ModelBuilder.Command
import com.lightbend.akkasls.codegen.ModelBuilder.EventSourcedEntity
import com.lightbend.akkasls.codegen.ModelBuilder.State
import com.lightbend.akkasls.codegen.ModelBuilder.ValueEntity

/**
 * Responsible for generating Java source from an entity model
 */
object EntityServiceSourceGenerator {
  import SourceGenerator._

  /**
   * Generate Java source from entities where the target source and test source directories have no existing source.
   * Note that we only generate tests for entities where we are successful in generating an entity. The user may
   * not want a test otherwise.
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
      mainClassName: String
  ): Iterable[Path] = {
    val packageName = entity.fqn.parent.javaPackage
    val className = entity.fqn.name
    val packagePath = packageAsPath(packageName)

    val implClassName = className
    val implSourcePath =
      sourceDirectory.resolve(packagePath.resolve(implClassName + ".java"))

    val interfaceClassName = "Abstract" + className
    val interfaceSourcePath =
      generatedSourceDirectory.resolve(packagePath.resolve(interfaceClassName + ".java"))

    interfaceSourcePath.getParent.toFile.mkdirs()
    Files.write(
      interfaceSourcePath,
      interfaceSource(service, entity, packageName, className).getBytes(Charsets.UTF_8)
    )

    val handlerClassName = className + "Handler"
    val handlerSourcePath = {
      val path = generatedSourceDirectory.resolve(packagePath.resolve(handlerClassName + ".java"))
      path.getParent.toFile.mkdirs()
      Files.write(
        path,
        handlerSource(service, entity, packageName, className).getBytes(Charsets.UTF_8)
      )
      path
    }

    val providerClassName = className + "Provider"
    val providerSourcePath = {
      val path = generatedSourceDirectory.resolve(packagePath.resolve(providerClassName + ".java"))
      path.getParent.toFile.mkdirs()
      Files.write(
        path,
        providerSource(service, entity, packageName, className).getBytes(Charsets.UTF_8)
      )
      path
    }

    if (!implSourcePath.toFile.exists()) {
      // We're going to generate an entity - let's see if we can generate its test...
      val testClassName = className + "Test"
      val testSourcePath =
        testSourceDirectory.resolve(packagePath.resolve(testClassName + ".java"))
      val testSourceFiles = Nil // FIXME add new unit test generation

      // ...and then its integration test
      val integrationTestClassName = className + "IntegrationTest"
      val integrationTestSourcePath =
        integrationTestSourceDirectory
          .resolve(packagePath.resolve(integrationTestClassName + ".java"))
      val integrationTestSourceFiles = if (!integrationTestSourcePath.toFile.exists()) {
        integrationTestSourcePath.getParent.toFile.mkdirs()
        Files.write(
          integrationTestSourcePath,
          integrationTestSource(
            mainClassPackageName,
            mainClassName,
            service,
            entity,
            packageName,
            integrationTestClassName
          ).getBytes(Charsets.UTF_8)
        )
        List(integrationTestSourcePath)
      } else {
        List.empty
      }

      // Now we generate the entity
      implSourcePath.getParent.toFile.mkdirs()
      Files.write(
        implSourcePath,
        source(
          service,
          entity,
          packageName,
          implClassName,
          interfaceClassName,
          entity.entityType
        ).getBytes(Charsets.UTF_8)
      )

      Vector(implSourcePath, interfaceSourcePath) ++ testSourceFiles ++ integrationTestSourceFiles :+ providerSourcePath :+ handlerSourcePath
    } else {
      Vector(interfaceSourcePath) :+ providerSourcePath :+ handlerSourcePath
    }
  }

  private[codegen] def source(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.Entity,
      packageName: String,
      className: String,
      interfaceClassName: String,
      entityType: String
  ): String = {
    entity match {
      case eventSourcedEntity: EventSourcedEntity =>
        eventSourcedEntitySource(service, eventSourcedEntity, packageName, className, interfaceClassName)
      case valueEntity: ValueEntity =>
        ValueEntitySourceGenerator.valueEntitySource(service, valueEntity, packageName, className)
    }
  }

  private[codegen] def eventSourcedEntityHandler(service: ModelBuilder.EntityService,
                                                 entity: ModelBuilder.EventSourcedEntity,
                                                 packageName: String,
                                                 className: String): String = {

    val imports = generateImports(
      service.commands,
      entity.state,
      packageName,
      otherImports = Seq(
        "com.akkaserverless.javasdk.eventsourcedentity.CommandContext",
        "com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity",
        "com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityHandler"
      )
    )

    val serviceApiOuterClass = service.fqn.parent.javaOuterClassname
    val outerClassAndState = s"${entity.fqn.parent.javaOuterClassname}.${entity.state.fqn.name}"

    val eventCases = {
      if (entity.events.isEmpty)
        List(s"throw new EventSourcedEntityHandler.EventHandlerNotFound(event.getClass());")
      else
        entity.events.zipWithIndex.map {
          case (evt, i) =>
            val eventType = s"${entity.fqn.parent.javaOuterClassname}.${evt.fqn.name}"
            s"""|${if (i == 0) "" else "} else "}if (event instanceof $eventType) {
              |  return entity().${lowerFirst(evt.fqn.name)}(state, ($eventType) event);""".stripMargin
        }.toSeq :+
        s"""|} else {
          |  throw new EventSourcedEntityHandler.EventHandlerNotFound(event.getClass());
          |}""".stripMargin
    }

    val commandCases = service.commands
      .map { cmd =>
        val methodName = cmd.fqn.name
        val inputType = s"$serviceApiOuterClass.${cmd.inputType.name}"
        s"""|case "$methodName":
            |  return entity().${lowerFirst(methodName)}(state, ($inputType) command);
            |""".stripMargin
      }

    s"""|$managedCodeCommentString
        |package $packageName;
        |
        |$imports
        |
        |/**
        | * An event sourced entity handler that is the glue between the Protobuf service <code>${service.fqn.name}</code>
        | * and the command and event handler methods in the <code>${entity.fqn.name}</code> class.
        | */
        |public class ${className}Handler extends EventSourcedEntityHandler<$outerClassAndState, ${entity.fqn.name}> {
        |
        |  public ${className}Handler(${entity.fqn.name} entity) {
        |    super(entity);
        |  }
        |
        |  @Override
        |  public $outerClassAndState handleEvent($outerClassAndState state, Object event) {
        |    ${Syntax.indent(eventCases, 4)}
        |  }
        |
        |  @Override
        |  public EventSourcedEntity.Effect<?> handleCommand(
        |      String commandName, $outerClassAndState state, Object command, CommandContext context) {
        |    switch (commandName) {
        |
        |      ${Syntax.indent(commandCases, 6)}
        |
        |      default:
        |        throw new EventSourcedEntityHandler.CommandHandlerNotFound(commandName);
        |    }
        |  }
        |}""".stripMargin

  }

  private[codegen] def eventSourcedEntityProvider(service: ModelBuilder.EntityService,
                                                  entity: ModelBuilder.EventSourcedEntity,
                                                  packageName: String,
                                                  className: String): String = {
    val relevantTypes = {
      service.commands.flatMap { cmd =>
        cmd.inputType :: cmd.outputType :: Nil
      }.toSeq :+ entity.state.fqn
    }

    val imports = generateImports(
      relevantTypes,
      packageName,
      otherImports = Seq(
          "com.akkaserverless.javasdk.eventsourcedentity.EventSourcedContext",
          "com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityOptions",
          "com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityProvider",
          "com.google.protobuf.Descriptors",
          "java.util.function.Function"
        ) ++ relevantTypes.map(_.descriptorImport)
    )

    val descriptors =
      (collectRelevantTypes(relevantTypes, service.fqn)
        .map(d => s"${d.parent.javaOuterClassname}.getDescriptor()") :+ s"${service.fqn.parent.javaOuterClassname}.getDescriptor()").distinct.sorted

    s"""|$managedCodeCommentString
        |package $packageName;
        |
        |$imports
        |
        |/**
        | * An event sourced entity provider that defines how to register and create the entity for
        | * the Protobuf service <code>${service.fqn.name}</code>.
        | *
        | * Should be used with the <code>register</code> method in {@link com.akkaserverless.javasdk.AkkaServerless}.
        | */
        |public class ${className}Provider implements EventSourcedEntityProvider<${entity.fqn.parent.javaOuterClassname}.${entity.state.fqn.name}, $className> {
        |
        |  private final Function<EventSourcedContext, $className> entityFactory;
        |  private final EventSourcedEntityOptions options;
        |
        |  /** Factory method of ${className}Provider */
        |  public static ${className}Provider of(Function<EventSourcedContext, $className> entityFactory) {
        |    return new ${className}Provider(entityFactory, EventSourcedEntityOptions.defaults());
        |  }
        | 
        |  private ${className}Provider(
        |      Function<EventSourcedContext, $className> entityFactory,
        |      EventSourcedEntityOptions options) {
        |    this.entityFactory = entityFactory;
        |    this.options = options;
        |  }
        |
        |  @Override
        |  public final EventSourcedEntityOptions options() {
        |    return options;
        |  }
        | 
        |  public final ${className}Provider withOptions(EventSourcedEntityOptions options) {
        |    return new ${className}Provider(entityFactory, options);
        |  }
        |
        |  @Override
        |  public final Descriptors.ServiceDescriptor serviceDescriptor() {
        |    return ${service.fqn.parent.javaOuterClassname}.getDescriptor().findServiceByName("${service.fqn.name}");
        |  }
        |
        |  @Override
        |  public final String entityType() {
        |    return "${entity.entityType}";
        |  }
        |
        |  @Override
        |  public final ${className}Handler newHandler(EventSourcedContext context) {
        |    return new ${className}Handler(entityFactory.apply(context));
        |  }
        |
        |  @Override
        |  public final Descriptors.FileDescriptor[] additionalDescriptors() {
        |    return new Descriptors.FileDescriptor[] {
        |      ${Syntax.indent(descriptors.mkString(",\n"), 6)}
        |    };
        |  }
        |}""".stripMargin

  }

  private[codegen] def eventSourcedEntitySource(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.EventSourcedEntity,
      packageName: String,
      className: String,
      interfaceClassName: String
  ): String = {
    val messageTypes = service.commands.toSeq
        .flatMap(command => Seq(command.inputType, command.outputType)) ++
      entity.events.map(_.fqn) :+ entity.state.fqn

    val imports = generateImports(
      service.commands,
      entity.state,
      packageName,
      Seq(
        "com.akkaserverless.javasdk.eventsourcedentity.EventSourcedContext",
        "com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity",
        "com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity.Effect"
      )
    )

    val commandHandlers =
      service.commands
        .map { command =>
          s"""|@Override
              |public Effect<${qualifiedType(command.outputType)}> ${lowerFirst(command.fqn.name)}(${qualifiedType(
               entity.state.fqn
             )} currentState, ${qualifiedType(command.inputType)} ${lowerFirst(command.inputType.name)}) {
              |  return effects().error("The command handler for `${command.fqn.name}` is not implemented, yet");
              |}
              |""".stripMargin
        }

    val eventHandlers =
      entity match {
        case ModelBuilder.EventSourcedEntity(_, _, _, events) =>
          events.map { event =>
            s"""|@Override
                |public ${qualifiedType(entity.state.fqn)} ${lowerFirst(event.fqn.name)}(${qualifiedType(
                 entity.state.fqn
               )} currentState, ${qualifiedType(event.fqn)} ${lowerFirst(event.fqn.name)}) {
                |  throw new RuntimeException("The event handler for `${event.fqn.name}` is not implemented, yet");
                |}""".stripMargin
          }
      }

    s"""$generatedCodeCommentString
       |package $packageName;
       |
       |$imports
       |
       |/** An event sourced entity. */
       |public class $className extends ${interfaceClassName} {
       |
       |  @SuppressWarnings("unused")
       |  private final String entityId;
       |
       |  public $className(EventSourcedContext context) {
       |    this.entityId = context.entityId();
       |  }
       |
       |  @Override
       |  public ${qualifiedType(entity.state.fqn)} emptyState() {
       |    throw new UnsupportedOperationException("Not implemented yet, replace with your empty entity state");
       |  }
       |
       |  ${Syntax.indent(commandHandlers, num = 2)}
       |
       |  ${Syntax.indent(eventHandlers, num = 2)}
       |
       |}""".stripMargin
  }

  private[codegen] def interfaceSource(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.Entity,
      packageName: String,
      className: String
  ): String =
    entity match {
      case eventSourcedEntity: ModelBuilder.EventSourcedEntity =>
        abstractEventSourcedEntity(service, eventSourcedEntity, packageName, className)
      case valueEntity: ModelBuilder.ValueEntity =>
        ValueEntitySourceGenerator.abstractValueEntity(service, valueEntity, packageName, className)
    }

  private[codegen] def handlerSource(service: ModelBuilder.EntityService,
                                     entity: ModelBuilder.Entity,
                                     packageName: String,
                                     className: String): String = {
    entity match {
      case entity: ModelBuilder.EventSourcedEntity =>
        EntityServiceSourceGenerator.eventSourcedEntityHandler(service, entity, packageName, className)
      case entity: ValueEntity =>
        ValueEntitySourceGenerator.valueEntityHandler(service, entity, packageName, className)
    }
  }

  private[codegen] def providerSource(service: ModelBuilder.EntityService,
                                      entity: ModelBuilder.Entity,
                                      packageName: String,
                                      className: String): String = {
    entity match {
      case eventSourcedEntity: ModelBuilder.EventSourcedEntity =>
        eventSourcedEntityProvider(service, eventSourcedEntity, packageName, className)
      case valueEntity: ValueEntity =>
        ValueEntitySourceGenerator.valueEntityProvider(service, valueEntity, packageName, className)
    }
  }

  private[codegen] def abstractEventSourcedEntity(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.EventSourcedEntity,
      packageName: String,
      className: String
  ): String = {
    val imports = generateImports(
      service.commands,
      entity.state,
      packageName,
      Seq("com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity")
    )

    val commandHandlers = service.commands.map { command =>
      s"""|/** Command handler for "${command.fqn.name}". */
          |public abstract Effect<${qualifiedType(command.outputType)}> ${lowerFirst(command.fqn.name)}(${qualifiedType(
           entity.state.fqn
         )} currentState, ${qualifiedType(command.inputType)} ${lowerFirst(command.inputType.name)});
         |""".stripMargin
    }

    val eventHandlers = entity.events.map { event =>
      s"""|/** Event handler for "${event.fqn.name}". */
          |public abstract ${qualifiedType(entity.state.fqn)} ${lowerFirst(event.fqn.name)}(${qualifiedType(
           entity.state.fqn
         )} currentState, ${qualifiedType(
           event.fqn
         )} ${lowerFirst(
           event.fqn.name
         )});
         |""".stripMargin
    }

    s"""|$managedCodeCommentString
        |package $packageName;
        |
        |$imports
        |
        |/** An event sourced entity. */
        |public abstract class Abstract${className} extends EventSourcedEntity<${qualifiedType(entity.state.fqn)}> {
        |
        |  ${Syntax.indent(commandHandlers, num = 2)}
        |
        |  ${Syntax.indent(eventHandlers, num = 2)}
        |
        |}""".stripMargin
  }

  private[codegen] def integrationTestSource(
      mainClassPackageName: String,
      mainClassName: String,
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.Entity,
      packageName: String,
      testClassName: String
  ): String = {
    val serviceName = service.fqn.name

    val state =
      entity match {
        case es: ModelBuilder.EventSourcedEntity => es.state
        case ModelBuilder.ValueEntity(_, _, state) => state
      }

    val imports = generateImports(
      service.commands,
      state,
      packageName,
      List(service.fqn.parent.javaPackage + "." + serviceName + "Client") ++
      Seq(
        "com.akkaserverless.javasdk.testkit.junit.AkkaServerlessTestkitResource",
        "org.junit.ClassRule",
        "org.junit.Test",
        mainClassPackageName + "." + mainClassName
      )
    )

    val testCases = service.commands.map { command =>
      s"""|@Test
          |public void ${lowerFirst(command.fqn.name)}OnNonExistingEntity() throws Exception {
          |  // TODO: set fields in command, and provide assertions to match replies
          |  // client.${lowerFirst(command.fqn.name)}(${qualifiedType(command.inputType)}.newBuilder().build())
          |  //         .toCompletableFuture().get(2, SECONDS);
          |}
          |""".stripMargin

    }

    s"""$generatedCodeCommentString
      |package $packageName;
      |
      |$imports
      |
      |import static java.util.concurrent.TimeUnit.*;
      |
      |// Example of an integration test calling our service via the Akka Serverless proxy
      |// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
      |public class $testClassName {
      |
      |  /**
      |   * The test kit starts both the service container and the Akka Serverless proxy.
      |   */
      |  @ClassRule
      |  public static final AkkaServerlessTestkitResource testkit =
      |    new AkkaServerlessTestkitResource(${mainClassName}.createAkkaServerless());
      |
      |  /**
      |   * Use the generated gRPC client to call the service through the Akka Serverless proxy.
      |   */
      |  private final ${serviceName}Client client;
      |
      |  public ${testClassName}() {
      |    client = ${serviceName}Client.create(testkit.getGrpcClientSettings(), testkit.getActorSystem());
      |  }
      |
      |  ${Syntax.indent(testCases, num = 2)}
      |}""".stripMargin
  }

  private[codegen] def generateImports(types: Iterable[FullyQualifiedName],
                                       packageName: String,
                                       otherImports: Seq[String]): String = {
    val messageTypeImports = types
      .filterNot { typ =>
        typ.parent.javaPackage == packageName
      }
      .map(typeImport)

    (messageTypeImports ++ otherImports).toSeq.distinct.sorted
      .map(pkg => s"import $pkg;")
      .mkString("\n")
  }

  private[codegen] def generateImports(commands: Iterable[Command],
                                       state: State,
                                       packageName: String,
                                       otherImports: Seq[String]): String = {

    val types = commands.flatMap(command => Seq(command.inputType, command.outputType)).toSeq :+ state.fqn
    generateImports(types, packageName, otherImports)
  }

}
