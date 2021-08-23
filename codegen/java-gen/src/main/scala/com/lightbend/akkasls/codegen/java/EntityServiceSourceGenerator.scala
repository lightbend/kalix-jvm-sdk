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
    val handlerSourcePath = handlerSource(service, entity, packageName, className).map { doc =>
      val path = generatedSourceDirectory.resolve(packagePath.resolve(handlerClassName + ".java"))
      path.getParent.toFile.mkdirs()
      Files.write(
        path,
        doc.getBytes(Charsets.UTF_8)
      )
      path
    }

    val providerClassName = className + "Provider"
    val providerSourcePath = providerSource(service, entity, packageName, className).map { src =>
      val path = generatedSourceDirectory.resolve(packagePath.resolve(providerClassName + ".java"))
      path.getParent.toFile.mkdirs()
      Files.write(
        path,
        src.getBytes(Charsets.UTF_8)
      )
      path
    }

    if (!implSourcePath.toFile.exists()) {
      // We're going to generate an entity - let's see if we can generate its test...
      val testClassName = className + "Test"
      val testSourcePath =
        testSourceDirectory.resolve(packagePath.resolve(testClassName + ".java"))
      val testSourceFiles =
        if (!testSourcePath.toFile.exists()) {
          val src = testSource(service, entity, packageName, implClassName, testClassName)
          testSourcePath.getParent.toFile.mkdirs()
          Files.write(
            testSourcePath,
            src.getBytes(Charsets.UTF_8)
          )
          List(testSourcePath)
        } else {
          List.empty
        }

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

      List(implSourcePath, interfaceSourcePath) ++ testSourceFiles ++ integrationTestSourceFiles ++ providerSourcePath.toList ++ handlerSourcePath.toList
    } else {
      List(interfaceSourcePath) ++ providerSourcePath.toList ++ handlerSourcePath.toList
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
        eventSourcedEntitySource(service, eventSourcedEntity, packageName, className, interfaceClassName, entityType)
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
        "com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityBase",
        "com.akkaserverless.javasdk.impl.EntityExceptions",
        "com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityHandler",
        "com.google.protobuf.Any",
        "com.google.protobuf.InvalidProtocolBufferException"
      )
    )

    val serviceApiOuterClass = service.fqn.parent.javaOuterClassname
    // FIXME why do we support None state?
    val outerClassAndState =
      entity.state.map(s => s"${entity.fqn.parent.javaOuterClassname}.${s.fqn.name}").getOrElse("Object")

    val eventCases = entity.events.zipWithIndex.map {
        case (evt, i) =>
          val methodName = evt.fqn.name
          val eventType = s"${entity.fqn.parent.javaOuterClassname}.${evt.fqn.name}"
          s"""|${if (i == 0) "" else "} else "}if (event instanceof $eventType) {
              |  return entity().${lowerFirst(evt.fqn.name)}(state, ($eventType) event);""".stripMargin
      }.toSeq :+
      s"""|} else {
        |  throw new IllegalArgumentException("Unknown event type [" + event.getClass() + "]");
        |}""".stripMargin

    val commandCases = service.commands
      .map { cmd =>
        val methodName = cmd.fqn.name
        val inputType = s"$serviceApiOuterClass.${cmd.inputType.name}"
        s"""|case "$methodName":
            |  // FIXME could parsing to the right type also be pulled out of here?
            |  return entity().${lowerFirst(methodName)}(state, ${inputType}.parseFrom(command.getValue()));
            |""".stripMargin
      }

    s"""|$managedCodeCommentString
        |package $packageName;
        |
        |$imports
        |
        |/** An event sourced entity handler */
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
        |  public EventSourcedEntityBase.Effect<?> handleCommand(
        |      String commandName, $outerClassAndState state, Any command, CommandContext context) {
        |    try {
        |      switch (commandName) {
        |
        |        ${Syntax.indent(commandCases, 8)}
        |
        |        default:
        |          throw new EntityExceptions.EntityException(
        |              context.entityId(),
        |              context.commandId(),
        |              commandName,
        |              "No command handler found for command ["
        |                  + commandName
        |                  + "] on "
        |                  + entity().getClass());
        |      }
        |    } catch (InvalidProtocolBufferException ex) {
        |      // This is if command payload cannot be parsed
        |      throw new RuntimeException(ex);
        |    }
        |  }
        |}""".stripMargin

  }

  private[codegen] def eventSourcedEntityProvider(service: ModelBuilder.EntityService,
                                                  entity: ModelBuilder.EventSourcedEntity,
                                                  packageName: String,
                                                  className: String): String = {
    val relevantTypes = {
      entity.state.map(_.fqn).toList ++
      service.commands.flatMap { cmd =>
        cmd.inputType :: cmd.outputType :: Nil
      }
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
        ) ++ relevantTypes.map(fqn => fqn.parent.javaPackage + "." + fqn.parent.javaOuterClassname)
    )

    val descriptors =
      collectRelevantTypes(relevantTypes, service.fqn)
        .map(d => s"${d.parent.javaOuterClassname}.getDescriptor()")
        .distinct
        .sorted

    s"""|$managedCodeCommentString
        |package $packageName;
        |
        |$imports
        |
        |/** An event sourced entity provider */
        |public class ${className}Provider implements EventSourcedEntityProvider<${entity.state
         .map(s => s"${entity.fqn.parent.javaOuterClassname}.${s.fqn.name}")
         .getOrElse(
           "Object"
         )}, ${className}> {
        |
        |  private final Function<EventSourcedContext, ${className}> entityFactory;
        |  private final EventSourcedEntityOptions options;
        |
        |  /** Factory method of ${className}Provider */
        |  public static ${className}Provider of(Function<EventSourcedContext, ${className}> entityFactory) {
        |    return new ${className}Provider(entityFactory, EventSourcedEntityOptions.defaults());
        |  }
        | 
        |  private ${className}Provider(
        |      Function<EventSourcedContext, ${className}> entityFactory,
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
      interfaceClassName: String,
      entityType: String
  ): String = {
    val messageTypes = service.commands.toSeq
        .flatMap(command => Seq(command.inputType, command.outputType)) ++
      entity.state.toSeq.map(_.fqn) ++ entity.events.map(_.fqn)

    val imports = (messageTypes
      .filterNot(_.parent.javaPackage == packageName)
      .map(typeImport) ++
    Seq(
      "com.akkaserverless.javasdk.eventsourcedentity.EventSourcedContext",
      "com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityBase",
      "com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityBase.Effect"
    )).distinct.sorted

    pretty(
      initialisedCodeComment <> line <> line <>
      "package" <+> packageName <> semi <> line <>
      line <>
      ssep(
        imports.to[immutable.Seq].map(pkg => "import" <+> pkg <> semi),
        line
      ) <> line <>
      line <>
      "/** An event sourced entity. */" <> line <>
      `class`("public", s"$className extends $interfaceClassName") {
        "@SuppressWarnings" <> parens(dquotes("unused")) <> line <>
        "private" <+> "final" <+> "String" <+> "entityId" <> semi <> line <>
        line <>
        constructor(
          "public",
          className,
          List("EventSourcedContext" <+> "context")
        ) {
          "this.entityId" <+> equal <+> "context.entityId()" <> semi
        } <> line <>
        line <>
        (entity.state match {
          case Some(state) =>
            "@Override" <>
            line <>
            method(
              "public",
              qualifiedType(state.fqn),
              "emptyState",
              Nil,
              emptyDoc
            )(
              "throw new UnsupportedOperationException" <> parens(
                dquotes("Not implemented yet, replace with your empty entity state")
              ) <> semi
            ) <>
            line <>
            line <>
            ssep(
              service.commands.toSeq
                .map { command =>
                  "@Override" <>
                  line <>
                  method(
                    "public",
                    "Effect" <> angles(qualifiedType(command.outputType)),
                    lowerFirst(command.fqn.name),
                    List(
                      qualifiedType(state.fqn) <+> "currentState",
                      qualifiedType(command.inputType) <+> lowerFirst(command.inputType.name)
                    ),
                    emptyDoc
                  ) {
                    "return effects().error" <> parens(notImplementedError("command", command.fqn)) <> semi
                  }
                }
                .to[immutable.Seq],
              line <> line
            ) <> line <> line <>
            (entity match {
              case ModelBuilder.EventSourcedEntity(_, _, _, events) =>
                ssep(
                  events.toSeq
                    .map { event =>
                      "@Override" <>
                      line <>
                      method(
                        "public",
                        qualifiedType(state.fqn),
                        lowerFirst(event.fqn.name),
                        List(
                          qualifiedType(state.fqn) <+> "currentState",
                          qualifiedType(event.fqn) <+> lowerFirst(event.fqn.name)
                        ),
                        emptyDoc
                      ) {
                        "throw new RuntimeException" <> parens(
                          notImplementedError("event", event.fqn)
                        ) <> semi
                      }
                    }
                    .to[immutable.Seq],
                  line <> line
                )
              case _ => emptyDoc
            })

          case _ => emptyDoc
        })
      }
    ).layout
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
                                     className: String): Option[String] = {
    entity match {
      case entity: ModelBuilder.EventSourcedEntity =>
        Some(EntityServiceSourceGenerator.eventSourcedEntityHandler(service, entity, packageName, className))
      case entity: ValueEntity =>
        Some(ValueEntitySourceGenerator.valueEntityHandler(service, entity, packageName, className))
    }
  }

  private[codegen] def providerSource(service: ModelBuilder.EntityService,
                                      entity: ModelBuilder.Entity,
                                      packageName: String,
                                      className: String): Option[String] = {
    entity match {
      case eventSourcedEntity: ModelBuilder.EventSourcedEntity =>
        Some(eventSourcedEntityProvider(service, eventSourcedEntity, packageName, className))
      case valueEntity: ValueEntity =>
        Some(ValueEntitySourceGenerator.valueEntityProvider(service, valueEntity, packageName, className))
    }
  }

  private[codegen] def abstractEventSourcedEntity(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.EventSourcedEntity,
      packageName: String,
      className: String
  ): String = {
    val messageTypes = service.commands.toSeq
        .flatMap(command => Seq(command.inputType, command.outputType)) ++ entity.state.toSeq
        .map(_.fqn) ++ entity.events.map(_.fqn)

    val imports = (messageTypes
      .filterNot(_.parent.javaPackage == packageName)
      .map(typeImport) ++
    Seq(
      "com.akkaserverless.javasdk.eventsourcedentity.CommandHandler",
      "com.akkaserverless.javasdk.eventsourcedentity.EventHandler",
      "com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityBase"
    )).distinct.sorted

    pretty(
      managedCodeComment <> line <> line <>
      "package" <+> packageName <> semi <> line <>
      line <>
      ssep(
        imports.to[immutable.Seq].map(pkg => "import" <+> pkg <> semi),
        line
      ) <> line <>
      line <>
      (entity.state match {
        case Some(state) =>
          "/** An event sourced entity. */" <>
          line <>
          `class`("public abstract", s"Abstract$className extends EventSourcedEntityBase<${qualifiedType(state.fqn)}>") {
            line <>
            ssep(
              service.commands.toSeq
                .map { command =>
                  "@CommandHandler" <> line <>
                  abstractMethod(
                    "public",
                    "Effect" <> angles(qualifiedType(command.outputType)),
                    lowerFirst(command.fqn.name),
                    List(
                      qualifiedType(state.fqn) <+> "currentState",
                      qualifiedType(command.inputType) <+> lowerFirst(command.inputType.name)
                    )
                  ) <> semi
                }
                .to[immutable.Seq],
              line <> line
            ) <>
            line <>
            line <>
            ssep(
              entity.events.toSeq
                .map { event =>
                  "@EventHandler" <> line <>
                  abstractMethod(
                    "public",
                    qualifiedType(state.fqn),
                    lowerFirst(event.fqn.name),
                    List(
                      qualifiedType(state.fqn) <+> "currentState",
                      qualifiedType(event.fqn) <+> lowerFirst(event.fqn.name)
                    )
                  ) <> semi
                }
                .to[immutable.Seq],
              line <> line
            )
          }
        case _ => emptyDoc
      })
    ).layout
  }

  private[codegen] def testSource(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.Entity,
      packageName: String,
      implClassName: String,
      testClassName: String
  ): String = {
    val messageTypes =
      service.commands.flatMap(command => Seq(command.inputType, command.outputType)) ++ (entity match {
        case _: ModelBuilder.EventSourcedEntity =>
          Seq.empty
        case ModelBuilder.ValueEntity(_, _, state) => Seq(state.fqn)
      })

    val imports = (messageTypes.toSeq
      .filterNot(_.parent.javaPackage == packageName)
      .map(typeImport) ++ Seq(
      "org.junit.Test",
      "org.mockito.*"
    ) ++ (entity match {
      case _: EventSourcedEntity =>
        Seq("com.akkaserverless.javasdk.eventsourcedentity.CommandContext")
      case _: ValueEntity =>
        Seq("com.akkaserverless.javasdk.valueentity.CommandContext")
    })).distinct.sorted

    pretty(
      initialisedCodeComment <> line <> line <>
      "package" <+> packageName <> semi <> line <>
      line <>
      ssep(
        imports.to[immutable.Seq].map(pkg => "import" <+> pkg <> semi),
        line
      ) <> line <>
      line <>
      "import" <+> "static" <+> "org.junit.Assert.assertThrows" <> semi <> line <>
      line <>
      `class`("public", testClassName) {
        "private" <+> "String" <+> "entityId" <+> equal <+> """"entityId1"""" <> semi <> line <>
        "private" <+> implClassName <+> "entity" <> semi <> line <>
        "private" <+> (entity match {
          case ModelBuilder.ValueEntity(_, _, state) =>
            "CommandContext" <> angles(qualifiedType(state.fqn))
          case _ =>
            "CommandContext"
        }) <+> "context" <+> equal <+> "Mockito.mock(CommandContext.class)" <> semi <> line <>
        line <>
        ssep(
          service.commands.toSeq
            .map {
              command =>
                "@Test" <> line <>
                method(
                  "public",
                  "void",
                  lowerFirst(command.fqn.name) + "Test",
                  List.empty,
                  emptyDoc
                ) {
                  "entity" <+> equal <+> "new" <+> implClassName <> parens(
                    (entity match {
                      case _: ModelBuilder.ValueEntity => "context"
                      case _ => "entityId"
                    })
                  ) <> semi <> line <>
                  line <>
                  "// TODO: write your mock here" <> line <>
                  "// Mockito.when(context.[...]).thenReturn([...]);" <> line <>
                  line <>
                  "// TODO: set fields in command, and update assertions to verify implementation" <> line <>
                  "//" <+> "assertEquals" <> parens(
                    "[expected]" <> comma <>
                    line <> "//" <> indent("entity") <> dot <> lowerFirst(
                      command.fqn.name
                    ) <> lparen <>
                    qualifiedType(
                      command.inputType
                    ) <> dot <> "newBuilder().build(), context"
                  ) <> semi <> line <>
                  "//" <+> rparen <> semi <>
                  (entity match {
                    case _: ModelBuilder.EventSourcedEntity =>
                      line <>
                      line <>
                      "// TODO: if you wish to verify events:" <> line <>
                      "//" <> indent("Mockito.verify(context).emit(event)") <> semi
                    case _ => emptyDoc
                  })
                }
            }
            .to[immutable.Seq],
          line <> line
        )
      }
    ).layout
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

    val messageTypes =
      entity match {
        case _: ModelBuilder.EventSourcedEntity =>
          Seq.empty
        case ModelBuilder.ValueEntity(_, _, state) => Seq(state.fqn)
      }

    val imports = messageTypes
        .filterNot(_.parent.javaPackage == packageName)
        .map(typeImport) ++
      List(mainClassPackageName + "." + mainClassName) ++
      List(service.fqn.parent.javaPackage + "." + serviceName + "Client") ++
      Seq(
        "com.akkaserverless.javasdk.testkit.junit.AkkaServerlessTestkitResource",
        "org.junit.ClassRule",
        "org.junit.Test"
      ).distinct.sorted

    pretty(
      initialisedCodeComment <> line <> line <>
      "package" <+> packageName <> semi <> line <>
      line <>
      ssep(
        imports.to[immutable.Seq].map(pkg => "import" <+> pkg <> semi),
        line
      ) <> line <>
      line <>
      "import" <+> "static" <+> "java.util.concurrent.TimeUnit.*" <> semi <> line <>
      line <>
      """// Example of an integration test calling our service via the Akka Serverless proxy""" <> line <>
      """// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`""" <> line <>
      `class`("public", testClassName) {
        line <>
        "/**" <> line <>
        " * The test kit starts both the service container and the Akka Serverless proxy." <> line <>
        " */" <> line <>
        "@ClassRule" <> line <>
        field(
          "public" <+> "static" <+> "final",
          "AkkaServerlessTestkitResource",
          "testkit",
          assignmentSeparator = Some(" ")
        ) {
          "new" <+> "AkkaServerlessTestkitResource" <> parens(mainClassName + ".SERVICE") <> semi
        } <> line <>
        line <>
        "/**" <> line <>
        " * Use the generated gRPC client to call the service through the Akka Serverless proxy." <> line <>
        " */" <> line <>
        field(
          "private" <+> "final",
          serviceName + "Client",
          "client",
          assignmentSeparator = None
        )(emptyDoc) <> semi <> line <>
        line <>
        constructor(
          "public",
          testClassName,
          List.empty
        ) {
          "client" <+> equal <+> serviceName <> "Client" <> dot <> "create" <> parens(
            ssep(
              List(
                "testkit" <> dot <> "getGrpcClientSettings" <> parens(
                  emptyDoc
                ),
                "testkit" <> dot <> "getActorSystem" <> parens(emptyDoc)
              ),
              comma <> space
            )
          ) <> semi
        } <> line <>
        line <>
        ssep(
          service.commands.toSeq
            .map {
              command =>
                "@Test" <> line <>
                method(
                  "public",
                  "void",
                  lowerFirst(command.fqn.name) + "OnNonExistingEntity",
                  List.empty,
                  "throws" <+> "Exception" <> space
                ) {
                  "// TODO: set fields in command, and provide assertions to match replies" <> line <>
                  "//" <+> "client" <> dot <> lowerFirst(command.fqn.name) <> parens(
                    qualifiedType(
                      command.inputType
                    ) <> dot <> "newBuilder().build()"
                  ) <> line <>
                  "//" <+> indent(
                    dot <> "toCompletableFuture" <> parens(emptyDoc) <> dot <> "get" <> parens(
                      ssep(List("2", "SECONDS"), comma <> space)
                    ) <> semi,
                    8
                  )
                }
            }
            .to[immutable.Seq],
          line <> line
        )
      }
    ).layout
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
                                       state: Option[State],
                                       packageName: String,
                                       otherImports: Seq[String]): String = {

    val types = state.map(_.fqn) ++ commands.flatMap(command => Seq(command.inputType, command.outputType))
    generateImports(types, packageName, otherImports)
  }
}
