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
  * Responsible for generating JavaScript source from view service model
  */
object ViewServiceSourceGenerator {

  import SourceGenerator._

  private val ProtoExt = ".proto"

  private val ProtoNs = "proto"

  def generate(
      entity: ModelBuilder.Entity,
      service: ModelBuilder.ViewService,
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

    if (!sourcePath.toFile.exists()) {
      // Now we generate the entity
      val _ = sourcePath.getParent.toFile.mkdirs()
      val _ = Files.write(
        sourcePath,
        source(
          allProtoSources,
          protobufSourceDirectory,
          sourceDirectory,
          generatedSourceDirectory,
          service
        ).layout
          .getBytes(Charsets.UTF_8)
      )
      List(sourcePath, typedefSourcePath)
    } else {
      List(typedefSourcePath)
    }
  }

  private[codegen] def source(
      protoSources: Iterable[Path],
      protobufSourceDirectory: Path,
      sourceDirectory: Path,
      generatedSourceDirectory: Path,
      service: ModelBuilder.ViewService
  ): Document = {
    val typedefPath =
      sourceDirectory.toAbsolutePath
        .relativize(generatedSourceDirectory.toAbsolutePath)
        .resolve(service.fqn.name.toLowerCase())
        .toString

    pretty(
      "import" <+> braces(" View ") <+> "from" <+> dquotes(
        "@lightbend/akkaserverless-javascript-sdk"
      ) <> semi <> line <>
      line <>
      blockComment(
        Seq[Doc](
          "Type definitions.",
          "These types have been generated based on your proto source.",
          "A TypeScript aware editor such as VS Code will be able to leverage them to provide hinting and validation.",
          emptyDoc,
          service.fqn.name <> semi <+> "a strongly typed extension of View derived from your proto source",
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
      "const view" <+> equal <+> "new" <+> "View" <> parens(
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
                  "serializeFallbackToJson" <> colon <+> "true",
                  "viewId" <> colon <+> dquotes(service.viewId)
                ),
                comma <> line
              )
            ) <> line
          )
        ) <> line
      ) <> semi <> line <>
      line <>
      "view.setUpdateHandlers" <> parens(
        braces(
          nest(
            line <>
            ssep(
              service.updates.toSeq.map { update =>
                update.fqn.name <> parens(
                  "event, state, ctx"
                ) <+> braces(
                  nest(
                    line <>
                    "return ctx.fail(\"The update handler for `" <> update.fqn.name <> "` is not implemented, yet\")" <> semi
                  ) <> line
                )
              },
              comma <> line
            )
          ) <> line
        )
      ) <> semi <> line <>
      line <>
      "export default view;"
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
}
