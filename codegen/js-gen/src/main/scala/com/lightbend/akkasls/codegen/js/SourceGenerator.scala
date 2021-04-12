/*
 * Copyright (c) Lightbend Inc. 2021
 *
 */

package com.lightbend.akkasls.codegen
package js

import com.google.common.base.Charsets
import org.bitbucket.inkytonik.kiama.output.PrettyPrinter
import org.bitbucket.inkytonik.kiama.output.PrettyPrinterTypes.Document

import java.nio.file.{ Files, Path }
import java.util.stream.Collectors
import scala.jdk.CollectionConverters._

/**
  * Responsible for generating JavaScript source from an entity model
  */
object SourceGenerator extends PrettyPrinter {

  override val defaultIndent = 2

  private val ProtoExt = ".proto"

  /**
    * Generate JavaScript source from entities where the target source and test source directories have no existing source.
    * Note that we only generate tests for entities where we are successful in generating an entity. The user may
    * not want a test otherwise.
    *
    * Also generates a main source file if it does not already exist.
    *
    * Impure.
    *
    * @param protobufDescriptor The path to the protobuf descriptor file
    * @param entities        The model of entity metadata to generate source file
    * @param protobufSourceDirectory A directory to read protobuf source files in.
    * @param sourceDirectory A directory to generate source files in, which can also containing existing source.
    * @param testSourceDirectory A directory to generate test source files in, which can also containing existing source.
    * @param indexFilename  The name of the index file e.g. index.js
    * @return A collection of paths addressing source files generated by this function
    */
  def generate(
      protobufDescriptor: Path,
      entities: Iterable[ModelBuilder.Entity],
      protobufSourceDirectory: Path,
      sourceDirectory: Path,
      testSourceDirectory: Path,
      indexFilename: String
  ): Iterable[Path] = {
    val allProtoSources = Files
      .walk(protobufSourceDirectory)
      .filter(p => Files.isRegularFile(p) && p.toString.endsWith(ProtoExt))
      .collect(Collectors.toList())
      .asScala
      .map(p => protobufSourceDirectory.toAbsolutePath.relativize(p.toAbsolutePath))
    entities.flatMap { case entity: ModelBuilder.EventSourcedEntity =>
      val entityFilename = name(entity.fullName).toLowerCase + ".js"
      val sourcePath     = sourceDirectory.resolve(entityFilename)
      if (!sourcePath.toFile.exists()) {
        // We're going to generate an entity - let's see if we can generate its test...
        val entityTestFilename = entityFilename.replace(".js", ".test.js")
        val testSourcePath =
          testSourceDirectory.resolve(entityTestFilename)
        val testSourceFiles = if (!testSourcePath.toFile.exists()) {
          val _ = testSourcePath.getParent.toFile.mkdirs()
          val _ = Files.write(
            testSourcePath,
            testSource(entity, testSourceDirectory, sourceDirectory).layout.getBytes(
              Charsets.UTF_8
            )
          )
          List(testSourcePath)
        } else {
          List.empty
        }

        // Now we generate the entity
        val _ = sourcePath.getParent.toFile.mkdirs()
        val _ = Files.write(
          sourcePath,
          source(
            allProtoSources,
            protobufSourceDirectory,
            sourceDirectory,
            entity
          ).layout
            .getBytes(Charsets.UTF_8)
        )

        List(sourcePath) ++ testSourceFiles
      } else {
        List.empty
      }
    } ++ {
      if (entities.nonEmpty) {
        // Generate a main source file is it is not there already
        val indexPath =
          sourceDirectory.resolve(indexFilename)
        if (!indexPath.toFile.exists()) {
          val _ = indexPath.getParent.toFile.mkdirs()
          val _ = Files.write(
            indexPath,
            indexSource(entities).layout.getBytes(
              Charsets.UTF_8
            )
          )
          List(indexPath)
        } else {
          List.empty
        }
      } else {
        List.empty
      }
    }
  }

  private[codegen] def source(
      protoSources: Iterable[Path],
      protobufSourceDirectory: Path,
      sourceDirectory: Path,
      entity: ModelBuilder.EventSourcedEntity
  ): Document =
    pretty(
      """import { EventSourcedEntity } from "@lightbend/akkaserverless-javascript-sdk"""" <> semi <> line <>
      line <>
      "const entity = new EventSourcedEntity" <> parens(
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
          dquotes(entity.fullName) <> comma <> line <>
          dquotes(name(entity.fullName).toLowerCase()) <> comma <> line <>
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
      "const commandHandlers" <+> equal <+> braces(
        nest(
          line <>
          ssep(
            entity.commands.toSeq.map { command =>
              name(command.fullname) <> parens(
                "command, state, ctx"
              ) <+> braces(
                nest(
                  line <>
                  "ctx.fail(\"The command handler for `" <> name(
                    command.fullname
                  ) <> "` is not implemented, yet\")" <> semi
                ) <> line
              )
            },
            comma <> line <> line
          )
        ) <> line
      ) <> line <>
      line <>
      "const eventHandlers" <+> equal <+> braces(
        nest(
          line <>
          ssep(
            entity.events.toSeq.map { event =>
              name(event) <> parens(
                "event, state"
              ) <+> braces(
                nest(
                  line <>
                  "return state"
                ) <> semi <> line
              )
            },
            comma <> line <> line
          )
        ) <> line
      ) <> line <>
      line <>
      "entity.setBehavior" <> parens(
        "state => " <> braces(
          nest(
            line <>
            "return" <+> braces(
              nest(
                line <>
                "commandHandlers" <> comma <> line <>
                "eventHandlers"
              ) <> line
            ) <> semi
          ) <> line
        )
      ) <> semi <> line <>
      line <>
      "export default entity;"
    )

  // TODO: Generate the test source
  private[codegen] def testSource(
      entity: ModelBuilder.EventSourcedEntity,
      testSourceDirectory: Path,
      sourceDirectory: Path
  ): Document = {

    val entityName = name(entity.fullName).toLowerCase
    pretty(
      """import { MockEventSourcedEntity } from "./testkit.js"""" <> semi <> line <>
      """import { expect } from "chai"""" <> semi <> line <>
      "import" <+> entityName <+> "from" <+> dquotes(
        testSourceDirectory.toAbsolutePath
          .relativize(sourceDirectory.toAbsolutePath)
          .resolve(s"$entityName.js")
          .toString
      ) <> semi <> line <>
      line <>

      "describe" <> parens(
        dquotes(name(entity.fullName)) <> comma <+> arrowFn(
          List.empty,
          "const" <+> "entityId" <+> equal <+> dquotes("entityId") <> semi <> line <>
          line <>
          ssep(
            entity.commands.map { command =>
              "describe" <> parens(
                dquotes(name(command.fullname)) <> comma <+> arrowFn(
                  List.empty,
                  "it" <> parens(
                    dquotes("should...") <> comma <+> arrowFn(
                      List.empty,
                      "const entity" <+> equal <+> "new MockEventSourcedEntity" <> parens(
                        entityName <> comma <+> "entityId"
                      ) <> semi <> line <>
                      "// TODO: you may want to set fields in addition to the entity id" <> line <>
                      "// const result" <+> equal <+> "entity.handleCommand" <> parens(
                        dquotes(name(command.fullname)) <> comma <+> braces(" entityId ")
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
                      ) <> semi <> line <>
                      "// expect" <> parens(
                        "entity.events"
                      ) <> dot <> "to" <> dot <> "deep" <> dot <> "equal" <> parens(
                        "[]"
                      ) <> semi
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

  private[codegen] def indexSource(
      entities: Iterable[ModelBuilder.Entity]
  ): Document =
    pretty(
      ssep(
        entities.map { case entity: ModelBuilder.EventSourcedEntity =>
          val entityName = name(entity.fullName).toLowerCase
          "import" <+> entityName <+> "from" <+> dquotes(s"./$entityName.js") <> semi
        }.toSeq,
        line
      ) <> line <> line <>
      ssep(
        entities.map { case entity: ModelBuilder.EventSourcedEntity =>
          name(entity.fullName).toLowerCase <> ".start()" <> semi
        }.toSeq,
        line
      )
    )

  private def name(`type`: String): String =
    `type`.reverse.takeWhile(_ != '.').reverse

  private def lowerFirst(text: String): String =
    text.headOption match {
      case Some(c) => c.toLower.toString + text.drop(1)
      case None    => ""
    }

  private def arrowFn(args: Seq[String], body: Doc) =
    parens(ssep(args.map(text), comma)) <+> "=>" <+> braces(nest(line <> body) <> line)

}
