/*
 * Copyright (c) Lightbend Inc. 2021
 *
 */

package com.lightbend.akkasls.codegen
package js

import com.google.common.base.Charsets
import org.bitbucket.inkytonik.kiama.output.PrettyPrinterTypes.Document

import java.nio.file.{ Files, Path }

/**
  * Responsible for generating JavaScript source from an entity model
  */
object EntityServiceSourceGenerator {

  import SourceGenerator._

  private val ProtoExt = ".proto"

  private val ProtoNs = "proto"

  def generate(
      entity: ModelBuilder.Entity,
      service: ModelBuilder.EntityService,
      protobufSourceDirectory: Path,
      sourceDirectory: Path,
      testSourceDirectory: Path,
      generatedSourceDirectory: Path,
      integrationTestSourceDirectory: Option[Path],
      indexFilename: String,
      allProtoSources: Iterable[Path]
  ) = {

    val entityFilename = entity.fqn.name.toLowerCase + ".js"
    val sourcePath     = sourceDirectory.resolve(entityFilename)

    val typedefFilename   = entity.fqn.name.toLowerCase + ".d.ts"
    val typedefSourcePath = generatedSourceDirectory.resolve(typedefFilename)
    val _                 = typedefSourcePath.getParent.toFile.mkdirs()
    val _ = Files.write(
      typedefSourcePath,
      typedefSource(service, entity).layout.getBytes(
        Charsets.UTF_8
      )
    )

    // We're going to generate an entity - let's see if we can generate its test...
    val entityTestFilename = entityFilename.replace(".js", ".test.js")
    val testSourcePath =
      testSourceDirectory.resolve(entityTestFilename)
    val testSourceFiles = if (!testSourcePath.toFile.exists()) {
      val _ = testSourcePath.getParent.toFile.mkdirs()
      val _ = Files.write(
        testSourcePath,
        testSource(service, entity, testSourceDirectory, sourceDirectory).layout.getBytes(
          Charsets.UTF_8
        )
      )
      List(testSourcePath)
    } else {
      List.empty
    }

    // Next, if an integration test directory is configured, we generate integration tests...
    val integrationTestSourceFiles = integrationTestSourceDirectory
      .map(_.resolve(entityTestFilename))
      .filterNot(_.toFile.exists())
      .map { integrationTestSourcePath =>
        val _ = integrationTestSourcePath.getParent.toFile.mkdirs()
        val _ = Files.write(
          integrationTestSourcePath,
          integrationTestSource(service, entity, testSourceDirectory, sourceDirectory).layout
            .getBytes(
              Charsets.UTF_8
            )
        )
        integrationTestSourcePath
      }

    val sourceFiles = if (!sourcePath.toFile.exists()) {
      // Now we generate the entity
      val _ = sourcePath.getParent.toFile.mkdirs()
      val _ = Files.write(
        sourcePath,
        source(
          allProtoSources,
          protobufSourceDirectory,
          sourceDirectory,
          generatedSourceDirectory,
          service,
          entity
        ).layout
          .getBytes(Charsets.UTF_8)
      )
      List(sourcePath, typedefSourcePath)
    } else {
      List(typedefSourcePath)
    }

    sourceFiles ++ testSourceFiles ++ integrationTestSourceFiles
  }

  private[codegen] def source(
      protoSources: Iterable[Path],
      protobufSourceDirectory: Path,
      sourceDirectory: Path,
      generatedSourceDirectory: Path,
      service: ModelBuilder.Service,
      entity: ModelBuilder.Entity
  ): Document = {
    val typedefPath =
      sourceDirectory.toAbsolutePath
        .relativize(generatedSourceDirectory.toAbsolutePath)
        .resolve(service.fqn.name.toLowerCase())
        .toString

    val entityType = entity match {
      case _: ModelBuilder.EventSourcedEntity => "EventSourcedEntity"
      case _: ModelBuilder.ValueEntity        => "ValueEntity"
    }
    pretty(
      "import" <+> "akkaserverless" <+> "from" <+> dquotes(
        "@lightbend/akkaserverless-javascript-sdk"
      ) <> semi <> line <>
      "const" <+> entityType <+> equal <+> "akkaserverless." <> entityType
      <> semi <> line <>
      line <>
      blockComment(
        Seq[Doc](
          "Type definitions.",
          "These types have been generated based on your proto source.",
          "A TypeScript aware editor such as VS Code will be able to leverage them to provide hinting and validation.",
          emptyDoc,
          "State; the serialisable and persistable state of the entity",
          typedef(
            "import" <> parens(
              dquotes(typedefPath)
            ) <> dot <> "State",
            "State"
          ),
          emptyDoc
        ) ++ (entity match {
          case _: ModelBuilder.EventSourcedEntity =>
            Seq[Doc](
              "Event; the union of all possible event types",
              typedef(
                "import" <> parens(
                  dquotes(typedefPath)
                ) <> dot <> "Event",
                "Event"
              ),
              emptyDoc
            )
          case _ => Seq.empty
        }) ++ Seq[Doc](
          service.fqn.name <> semi <+> "a strongly typed extension of" <+> entityType <+> "derived from your proto source",
          typedef(
            "import" <> parens(
              dquotes(typedefPath)
            ) <> dot <> service.fqn.name,
            service.fqn.name
          )
        ): _*
      ) <> line <>
      line <>
      blockComment(
        "@type" <+> service.fqn.name
      ) <> line <>
      "const entity" <+> equal <+> "new" <+> entityType <> parens(
        nest(
          line <>
          brackets(
            nest(
              line <>
              ssep(
                protoSources.map(p => dquotes(p.toString)).toList,
                comma <> line
              )
            ) <> line
          ) <> comma <> line <>
          dquotes(service.fqn.fullName) <> comma <> line <>
          dquotes(entity.entityType) <> comma <> line <>
          braces(
            nest(
              line <>
              ssep(
                (if (sourceDirectory != protobufSourceDirectory)
                   List(
                     "includeDirs" <> colon <+> brackets(
                       dquotes(protobufSourceDirectory.toString)
                     )
                   )
                 else List.empty) ++ List(
                  "serializeFallbackToJson" <> colon <+> "true"
                ),
                comma <> line
              )
            ) <> line
          )
        ) <> line
      ) <> semi <> line <>
      line <>
      "entity.setInitial" <> parens(
        "entityId => " <> parens("{}")
      ) <> semi <> line <>
      line <>
      (entity match {
        case ModelBuilder.EventSourcedEntity(_, _, _, events) =>
          "entity.setBehavior" <> parens(
            "state => " <> parens(
              braces(
                nest(
                  line <>
                  "commandHandlers" <> colon <+> braces(
                    nest(
                      line <>
                      ssep(
                        service.commands.toSeq.map { command =>
                          command.fqn.name <> parens(
                            "command, state, ctx"
                          ) <+> braces(
                            nest(
                              line <>
                              "return ctx.fail(\"The command handler for `" <> command.fqn.name <> "` is not implemented, yet\")" <> semi
                            ) <> line
                          )
                        },
                        comma <> line
                      )
                    ) <> line
                  ) <> comma <>
                  line <>
                  line <>
                  "eventHandlers" <> colon <+> braces(
                    nest(
                      line <>
                      ssep(
                        events.toSeq.map { event =>
                          event.fqn.name <> parens(
                            "event, state"
                          ) <+> braces(
                            nest(
                              line <>
                              "return state"
                            ) <> semi <> line
                          )
                        },
                        comma
                      )
                    ) <> line
                  )
                ) <> line
              )
            )
          ) <> semi
        case _: ModelBuilder.ValueEntity =>
          "entity.setCommandHandlers" <> parens(
            braces(
              nest(
                line <>
                ssep(
                  service.commands.toSeq.map { command =>
                    command.fqn.name <> parens(
                      "command, state, ctx"
                    ) <+> braces(
                      nest(
                        line <>
                        "return ctx.fail(\"The command handler for `" <> command.fqn.name <> "` is not implemented, yet\")" <> semi
                      ) <> line
                    )
                  },
                  comma <> line
                )
              ) <> line
            )
          ) <> semi
      }) <> line <>
      line <>
      "export default entity;"
    )
  }

  private[codegen] def typedefSource(
      service: ModelBuilder.Service,
      entity: ModelBuilder.Entity
  ): Document =
    pretty(
      "import" <+> braces(
        nest(line <> (entity match {
          case _: ModelBuilder.EventSourcedEntity =>
            "TypedEventSourcedEntity" <> comma <> line <>
              "EventSourcedCommandContext"
          case _: ModelBuilder.ValueEntity =>
            "TypedValueEntity" <> comma <> line <>
              "ValueEntityCommandContext"
        })) <> line
      ) <+> "from" <+> dquotes("../akkaserverless") <> semi <> line <>
      "import" <+> ProtoNs <+> "from" <+> dquotes("./proto") <> semi <> line <>
      line <>
      (entity match {
        case ModelBuilder.EventSourcedEntity(_, _, state, events) =>
          "export type State" <+> equal <+> state
            .map(state => typeReference(state.fqn))
            .getOrElse(text("unknown")) <> semi <> line <>
            "export type Event" <+> equal <> typeUnion(
              events.toSeq.map(_.fqn)
            ) <> semi
        case ModelBuilder.ValueEntity(_, _, state) =>
          "export type State" <+> equal <+> typeReference(state.fqn) <> semi
      }) <> line <>
      "export type Command" <+> equal <> typeUnion(
        service.commands.toSeq.map(_.inputType)
      ) <> semi <> line <>
      line <>
      (entity match {
        case ModelBuilder.EventSourcedEntity(_, _, _, events) =>
          "export type EventHandlers" <+> equal <+> braces(
            nest(
              line <>
              ssep(
                events.toSeq.map { event =>
                  event.fqn.name <> colon <+> parens(
                    nest(
                      line <>
                      "event" <> colon <+> typeReference(event.fqn) <> comma <> line <>
                      "state" <> colon <+> "State"
                    ) <> line
                  ) <+> "=>" <+> "State" <> semi
                },
                line
              )
            ) <> line
          ) <> semi <> line <>
            line
        case _: ModelBuilder.ValueEntity => emptyDoc
      }) <>
      "export type CommandHandlers" <+> equal <+> braces(
        nest(
          line <>
          ssep(
            service.commands.toSeq.map { command =>
              command.fqn.name <> colon <+> parens(
                nest(
                  line <>
                  "command" <> colon <+> typeReference(command.inputType) <> comma <> line <>
                  "state" <> colon <+> "State" <> comma <> line <>
                  "ctx" <> colon <+> (entity match {
                    case _: ModelBuilder.EventSourcedEntity => "EventSourcedCommandContext<Event>"
                    case _: ModelBuilder.ValueEntity        => "ValueEntityCommandContext<State>"
                  })
                ) <> line
              ) <+> "=>" <+> typeReference(command.outputType) <> semi
            },
            line
          )
        ) <> line
      ) <> semi <> line <>
      line <>
      "export type" <+> service.fqn.name <+> equal <+> (entity match {
        case _: ModelBuilder.EventSourcedEntity =>
          "TypedEventSourcedEntity" <> angles(
            nest(
              line <>
              ssep(Seq("State", "EventHandlers", "CommandHandlers"), comma <> line)
            ) <> line
          )
        case _: ModelBuilder.ValueEntity =>
          "TypedValueEntity" <> angles(
            nest(
              line <>
              ssep(Seq("State", "CommandHandlers"), comma <> line)
            ) <> line
          )
      }) <> semi <> line
    )

  private[codegen] def testSource(
      service: ModelBuilder.Service,
      entity: ModelBuilder.Entity,
      testSourceDirectory: Path,
      sourceDirectory: Path
  ): Document = {

    val entityName = entity.fqn.name.toLowerCase

    val entityMockType = entity match {
      case _: ModelBuilder.EventSourcedEntity => "MockEventSourcedEntity"
      case _: ModelBuilder.ValueEntity        => "MockValueEntity"
    }

    pretty(
      "import" <+> braces(
        " " <> entityMockType <> " "
      ) <+> "from" <+> dquotes("./testkit.js") <> semi <> line <>
      """import { expect } from "chai"""" <> semi <> line <>
      "import" <+> entityName <+> "from" <+> dquotes(
        testSourceDirectory.toAbsolutePath
          .relativize(sourceDirectory.toAbsolutePath)
          .resolve(s"$entityName.js")
          .toString
      ) <> semi <> line <>
      line <>

      "describe" <> parens(
        dquotes(service.fqn.name) <> comma <+> arrowFn(
          List.empty,
          "const" <+> "entityId" <+> equal <+> dquotes("entityId") <> semi <> line <>
          line <>
          ssep(
            service.commands.map { command =>
              "describe" <> parens(
                dquotes(command.fqn.name) <> comma <+> arrowFn(
                  List.empty,
                  "it" <> parens(
                    dquotes("should...") <> comma <+> arrowFn(
                      List.empty,
                      "const entity" <+> equal <+> "new" <+> entityMockType <> parens(
                        entityName <> comma <+> "entityId"
                      ) <> semi <> line <>
                      "// TODO: you may want to set fields in addition to the entity id" <> line <>
                      "// const result" <+> equal <+> "entity.handleCommand" <> parens(
                        dquotes(command.fqn.name) <> comma <+> braces(" entityId ")
                      ) <> semi <> line <>
                      line <>
                      "// expect" <> parens(
                        "result"
                      ) <> dot <> "to" <> dot <> "deep" <> dot <> "equal" <> parens(
                        braces("")
                      ) <> semi <> line <>
                      "// expect" <> parens(
                        "entity.error"
                      ) <> dot <> "to" <> dot <> "be" <> dot <> "undefined" <> semi <> line <>
                      "// expect" <> parens(
                        "entity.state"
                      ) <> dot <> "to" <> dot <> "deep" <> dot <> "equal" <> parens(
                        braces("")
                      ) <> semi <>
                      (entity match {
                        case _: ModelBuilder.EventSourcedEntity =>
                          line <>
                            "// expect" <> parens(
                              "entity.events"
                            ) <> dot <> "to" <> dot <> "deep" <> dot <> "equal" <> parens(
                              "[]"
                            ) <> semi
                        case _ => emptyDoc
                      })
                    )
                  ) <> semi
                )
              ) <> semi
            }.toSeq,
            line <> line
          )
        )
      ) <> semi
    )
  }

  private[codegen] def integrationTestSource(
      service: ModelBuilder.Service,
      entity: ModelBuilder.Entity,
      testSourceDirectory: Path,
      sourceDirectory: Path
  ): Document = {

    val entityName = entity.fqn.name.toLowerCase

    val entityMockType = entity match {
      case _: ModelBuilder.EventSourcedEntity => "MockEventSourcedEntity"
      case _: ModelBuilder.ValueEntity        => "MockValueEntity"
    }

    pretty(
      "import" <+> "akkaserverless" <+> "from" <+> dquotes(
        "@lightbend/akkaserverless-javascript-sdk"
      ) <> semi <> line <>
      """import { expect } from "chai"""" <> semi <> line <>
      "import" <+> entityName <+> "from" <+> dquotes(
        testSourceDirectory.toAbsolutePath
          .relativize(sourceDirectory.toAbsolutePath)
          .resolve(s"$entityName.js")
          .toString
      ) <> semi <> line <>
      line <>
      "const" <+> "testkit" <+> equal <+> "new" <+> "akkaserverless.IntegrationTestkit" <> parens(
        emptyDoc
      ) <> semi <> line <>
      "testkit" <> dot <> "addComponent" <> parens(entityName) <> semi <> line <>
      line <>
      "const" <+> "client" <+> equal <+> parens(
        emptyDoc
      ) <+> "=>" <+> "testkit.clients" <> dot <> service.fqn.name <> semi <> line <>
      line <>
      "describe" <> parens(
        dquotes(service.fqn.name) <> comma <+> "function" <> parens(emptyDoc) <+> braces(
          nest(
            line <>
            "this.timeout" <> parens("60000") <> semi <> line <>
            line <>
            "before" <> parens(
              "done" <+> "=>" <+> "testkit.start" <> parens("done")
            ) <> semi <> line <>
            "after" <> parens(
              "done" <+> "=>" <+> "testkit.shutdown" <> parens("done")
            ) <> semi <> line <>
            line <>
            ssep(
              service.commands.map { command =>
                "describe" <> parens(
                  dquotes(command.fqn.name) <> comma <+> arrowFn(
                    List.empty,
                    "it" <> parens(
                      dquotes("should...") <> comma <+> "async" <+> arrowFn(
                        List.empty,
                        "// TODO: populate command payload, and provide assertions to match replies" <> line <>
                        "//" <+> "const" <+> "result" <+> equal <+> "await" <+> "client" <> parens(
                          emptyDoc
                        ) <> dot <> lowerFirst(
                          command.fqn.name
                        ) <> parens(braces(emptyDoc)) <> semi
                      )
                    ) <> semi
                  )
                ) <> semi
              }.toSeq,
              line
            )
          ) <> line
        )
      ) <> semi
    )
  }
}
