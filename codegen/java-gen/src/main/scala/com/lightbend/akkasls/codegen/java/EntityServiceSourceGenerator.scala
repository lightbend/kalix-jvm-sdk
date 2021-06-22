/*
 * Copyright (c) Lightbend Inc. 2021
 *
 */

package com.lightbend.akkasls.codegen
package java

import com.google.common.base.Charsets
import org.bitbucket.inkytonik.kiama.output.PrettyPrinterTypes.Document

import _root_.java.nio.file.{ Files, Path }
import com.lightbend.akkasls.codegen.ModelBuilder.EventSourcedEntity
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
    val className   = entity.fqn.name
    val packagePath = packageAsPath(packageName)

    val implClassName = className + "Impl"
    val implSourcePath =
      sourceDirectory.resolve(packagePath.resolve(implClassName + ".java"))

    val interfaceClassName = className + "Interface"
    val interfaceSourcePath =
      generatedSourceDirectory.resolve(packagePath.resolve(interfaceClassName + ".java"))

    val _ = interfaceSourcePath.getParent.toFile.mkdirs()
    val _ = Files.write(
      interfaceSourcePath,
      interfaceSource(service, entity, packageName, className).layout.getBytes(
        Charsets.UTF_8
      )
    )

    if (!implSourcePath.toFile.exists()) {
      // We're going to generate an entity - let's see if we can generate its test...
      val testClassName = className + "Test"
      val testSourcePath =
        testSourceDirectory.resolve(packagePath.resolve(testClassName + ".java"))
      val testSourceFiles = if (!testSourcePath.toFile.exists()) {
        val _ = testSourcePath.getParent.toFile.mkdirs()
        val _ = Files.write(
          testSourcePath,
          testSource(service, entity, packageName, implClassName, testClassName).layout
            .getBytes(
              Charsets.UTF_8
            )
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
        val _ = integrationTestSourcePath.getParent.toFile.mkdirs()
        val _ = Files.write(
          integrationTestSourcePath,
          integrationTestSource(
            mainClassPackageName,
            mainClassName,
            service,
            entity,
            packageName,
            integrationTestClassName
          ).layout
            .getBytes(
              Charsets.UTF_8
            )
        )
        List(integrationTestSourcePath)
      } else {
        List.empty
      }

      // Now we generate the entity
      val _ = implSourcePath.getParent.toFile.mkdirs()
      val _ = Files.write(
        implSourcePath,
        source(
          service,
          entity,
          packageName,
          implClassName,
          interfaceClassName,
          entity.entityType
        ).layout.getBytes(
          Charsets.UTF_8
        )
      )

      List(implSourcePath, interfaceSourcePath) ++ testSourceFiles ++ integrationTestSourceFiles
    } else {
      List(interfaceSourcePath)
    }
  }

  private[codegen] def source(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.Entity,
      packageName: String,
      className: String,
      interfaceClassName: String,
      entityType: String
  ): Document = {
    val messageTypes = service.commands.toSeq.flatMap(command =>
      Seq(command.inputType, command.outputType)
    ) ++ (entity match {
      case ModelBuilder.EventSourcedEntity(_, _, state, events) =>
        state.toSeq.map(_.fqn) ++ events.map(_.fqn)
      case ModelBuilder.ValueEntity(_, _, state) => Seq(state.fqn)
    })

    val imports = (messageTypes
      .filterNot(_.parent.javaPackage == packageName)
      .map(typeImport) ++
      (entity match {
        case _: ModelBuilder.EventSourcedEntity =>
          Seq(
            "com.akkaserverless.javasdk.EntityId",
            "com.akkaserverless.javasdk.eventsourcedentity.*"
          )
        case _: ModelBuilder.ValueEntity =>
          Seq(
            "com.akkaserverless.javasdk.EntityId",
            "com.akkaserverless.javasdk.valueentity.*"
          )
      })).distinct.sorted

    pretty(
      "package" <+> packageName <> semi <> line <>
      line <>
      ssep(
        imports.map(pkg => "import" <+> pkg <> semi),
        line
      ) <> line <>
      line <>
      (entity match {
        case _: ModelBuilder.EventSourcedEntity =>
          "/** An event sourced entity. */" <> line <>
            "@EventSourcedEntity" <> parens(
              "entityType" <+> equal <+> dquotes(entityType)
            )
        case _: ModelBuilder.ValueEntity =>
          "/** A value entity. */" <> line <>
            "@ValueEntity" <> parens(
              "entityType" <+> equal <+> dquotes(entityType)
            )
      }) <> line <>
      `class`("public", s"$className extends $interfaceClassName") {
        "@SuppressWarnings" <> parens(dquotes("unused")) <> line <>
        "private" <+> "final" <+> "String" <+> "entityId" <> semi <> line <>
        line <>
        constructor(
          "public",
          className,
          List("@EntityId" <+> "String" <+> "entityId")
        ) {
          "this.entityId" <+> equal <+> "entityId" <> semi
        } <> line <>
        line <>
        (entity match {
          case ModelBuilder.EventSourcedEntity(_, _, Some(state), _) =>
            "@Override" <>
              line <>
              method(
                "public",
                qualifiedType(state.fqn),
                "snapshot",
                List.empty,
                emptyDoc
              ) {
                "// TODO: produce state snapshot here" <> line <>
                "return" <+> qualifiedType(
                  state.fqn
                ) <> dot <> "newBuilder().build()" <> semi
              } <> line <>
              line <>
              "@Override" <>
              line <>
              method(
                "public",
                "void",
                "handleSnapshot",
                List(
                  qualifiedType(state.fqn) <+> "snapshot"
                ),
                emptyDoc
              ) {
                "// TODO: restore state from snapshot here" <> line
              } <> line <> line
          case _ => emptyDoc
        }) <>
        ssep(
          service.commands.toSeq.map { command =>
            "@Override" <>
            line <>
            method(
              "protected",
              qualifiedType(command.outputType),
              lowerFirst(command.fqn.name),
              List(
                qualifiedType(command.inputType) <+> "command",
                (entity match {
                  case ModelBuilder.ValueEntity(_, _, state) =>
                    "CommandContext" <> angles(qualifiedType(state.fqn))
                  case _ => text("CommandContext")
                }) <+> "ctx"
              ),
              emptyDoc
            ) {
              "throw ctx.fail" <> parens(notImplementedError("command", command.fqn)) <> semi
            }
          },
          line <> line
        ) <>
        (entity match {
          case ModelBuilder.EventSourcedEntity(_, _, _, events) =>
            line <>
              line <>
              ssep(
                events.toSeq.map { event =>
                  "@Override" <>
                  line <>
                  method(
                    "public",
                    "void",
                    lowerFirst(event.fqn.name),
                    List(
                      qualifiedType(event.fqn) <+> "event"
                    ),
                    emptyDoc
                  ) {
                    "throw new RuntimeException" <> parens(
                      notImplementedError("event", event.fqn)
                    ) <> semi
                  }
                },
                line <> line
              )
          case _ => emptyDoc
        })
      }
    )
  }

  private[codegen] def interfaceSource(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.Entity,
      packageName: String,
      className: String
  ): Document = {
    val messageTypes = service.commands.toSeq.flatMap(command =>
      Seq(command.inputType, command.outputType)
    ) ++ (entity match {
      case ModelBuilder.EventSourcedEntity(_, _, state, events) =>
        state.toSeq.map(_.fqn) ++ events.map(_.fqn)
      case ModelBuilder.ValueEntity(_, _, state) => Seq(state.fqn)
    })

    val imports = (messageTypes
      .filterNot(_.parent.javaPackage == packageName)
      .map(typeImport) ++
      Seq(
        "com.akkaserverless.javasdk.EntityId",
        "com.akkaserverless.javasdk.Reply"
      ) ++ (entity match {
        case _: ModelBuilder.EventSourcedEntity =>
          Seq(
            "com.akkaserverless.javasdk.eventsourcedentity.*"
          )
        case _: ModelBuilder.ValueEntity =>
          Seq(
            "com.akkaserverless.javasdk.valueentity.*"
          )
      })).distinct.sorted

    pretty(
      "package" <+> packageName <> semi <> line <>
      line <>
      ssep(
        imports.map(pkg => "import" <+> pkg <> semi),
        line
      ) <> line <>
      line <>
      (entity match {
        case _: ModelBuilder.EventSourcedEntity => "/** An event sourced entity. */"
        case _: ModelBuilder.ValueEntity        => "/** A value entity. */"
      }) <> line <>
      `class`("public abstract", className + "Interface") {
        line <>
        `class`("public", "CommandNotImplementedException", Some("UnsupportedOperationException"))(
          constructor("public", "CommandNotImplementedException", Seq.empty)(
            "super" <> parens(
              dquotes(
                "You have either created a new command or removed the handling of an existing command. Please declare a method in your \\\"impl\\\" class for this command."
              )
            ) <> semi
          )
        ) <> line <>
        line <>
        (entity match {
          case ModelBuilder.EventSourcedEntity(_, _, Some(state), _) =>
            "@Snapshot" <>
              line <>
              abstractMethod(
                "public",
                qualifiedType(state.fqn),
                "snapshot",
                List.empty
              ) <> semi <> line <>
              line <>
              "@SnapshotHandler" <>
              line <>
              abstractMethod(
                "public",
                "void",
                "handleSnapshot",
                List(
                  qualifiedType(state.fqn) <+> "snapshot"
                )
              ) <> semi <> line <>
              line
          case _ => emptyDoc
        }) <>
        ssep(
          service.commands.toSeq.map { command =>
            "@CommandHandler" <> parens(
              "name" <+> equal <+> dquotes(command.fqn.name)
            ) <>
            line <>
            method(
              "public",
              "Reply" <> angles(qualifiedType(command.outputType)),
              lowerFirst(command.fqn.name) + "WithReply",
              List(
                qualifiedType(command.inputType) <+> "command",
                (entity match {
                  case ModelBuilder.ValueEntity(_, _, state) =>
                    "CommandContext" <> angles(qualifiedType(state.fqn))
                  case _ => text("CommandContext")
                }) <+> "ctx"
              ),
              emptyDoc
            )(
              "return" <+> "Reply.message" <> parens(
                lowerFirst(command.fqn.name) <> parens(
                  "command" <> comma <+> "ctx"
                )
              ) <> semi
            ) <> line <>
            line <>
            method(
              "protected",
              qualifiedType(command.outputType),
              lowerFirst(command.fqn.name),
              List(
                qualifiedType(command.inputType) <+> "command",
                (entity match {
                  case ModelBuilder.ValueEntity(_, _, state) =>
                    "CommandContext" <> angles(qualifiedType(state.fqn))
                  case _ => text("CommandContext")
                }) <+> "ctx"
              ),
              emptyDoc
            )(
              "return" <+>
              lowerFirst(command.fqn.name) <> parens("command") <> semi
            ) <> line <>
            line <>
            method(
              "protected",
              qualifiedType(command.outputType),
              lowerFirst(command.fqn.name),
              List(
                qualifiedType(command.inputType) <+> "command"
              ),
              emptyDoc
            )(
              "throw" <+> "new" <+> "CommandNotImplementedException" <> parens(emptyDoc) <> semi
            )
          },
          line <> line
        ) <>
        (entity match {
          case ModelBuilder.EventSourcedEntity(_, _, _, events) =>
            line <>
              line <>
              ssep(
                events.toSeq.map { event =>
                  "@EventHandler" <>
                  line <>
                  abstractMethod(
                    "public",
                    "void",
                    lowerFirst(event.fqn.name),
                    List(
                      qualifiedType(event.fqn) <+> "event"
                    )
                  ) <> semi
                },
                line <> line
              )
          case _ => emptyDoc
        })
      }
    )
  }

  private[codegen] def testSource(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.Entity,
      packageName: String,
      implClassName: String,
      testClassName: String
  ): Document = {
    val messageTypes =
      service.commands.flatMap(command =>
        Seq(command.inputType, command.outputType)
      ) ++ (entity match {
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
      "package" <+> packageName <> semi <> line <>
      line <>
      ssep(
        imports.map(pkg => "import" <+> pkg <> semi),
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
        "private class MockedContextFailure extends RuntimeException" <+> braces(
          emptyDoc
        ) <> semi <> line <>
        line <>
        ssep(
          service.commands.toSeq.map { command =>
            "@Test" <> line <>
            method(
              "public",
              "void",
              lowerFirst(command.fqn.name) + "Test",
              List.empty,
              emptyDoc
            ) {
              "entity" <+> equal <+> "new" <+> implClassName <> parens(
                "entityId"
              ) <> semi <> line <>
              line <>
              "Mockito.when" <> parens(
                "context.fail" <> parens(notImplementedError("command", command.fqn))
              ) <> line <>
              indent(
                dot <>
                "thenReturn" <> parens("new MockedContextFailure" <> parens(emptyDoc))
              ) <> semi <> line <>
              line <>
              "// TODO: set fields in command, and update assertions to match implementation" <> line <>
              "assertThrows" <> parens(
                "MockedContextFailure.class" <> comma <+>
                parens(emptyDoc) <+> "->" <+> braces(
                  nest(
                    line <> "entity" <> dot <> lowerFirst(
                      command.fqn.name
                    ) <> "WithReply" <> parens(
                      qualifiedType(
                        command.inputType
                      ) <> dot <> "newBuilder().build(), context"
                    ) <> semi
                  ) <> line
                )
              ) <> semi <>
              (entity match {
                case _: ModelBuilder.EventSourcedEntity =>
                  line <>
                    line <>
                    "// TODO: if you wish to verify events:" <> line <>
                    "//" <> indent("Mockito.verify(context).emit(event)") <> semi
                case _ => emptyDoc
              })
            }
          },
          line <> line
        )
      }
    )
  }

  private[codegen] def integrationTestSource(
      mainClassPackageName: String,
      mainClassName: String,
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.Entity,
      packageName: String,
      testClassName: String
  ): Document = {
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
      "package" <+> packageName <> semi <> line <>
      line <>
      ssep(
        imports.map(pkg => "import" <+> pkg <> semi),
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
          service.commands.toSeq.map { command =>
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
          },
          line <> line
        )
      }
    )
  }
}
