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

/**
 * Responsible for generating Java source from an entity model
 */
object ActionServiceSourceGenerator {
  import SourceGenerator._

  /**
   * Generate Java source from views where the target source and test source directories have no existing source.
   *
   * Also generates a main source file if it does not already exist.
   *
   * Impure.
   */
  def generate(
      service: ModelBuilder.ActionService,
      sourceDirectory: Path,
      testSourceDirectory: Path,
      integrationTestSourceDirectory: Path,
      generatedSourceDirectory: Path,
      mainClassPackageName: String,
      mainClassName: String
  ): Iterable[Path] = {
    val packageName = service.fqn.parent.javaPackage
    val className = service.fqn.name
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
      interfaceSource(service, packageName, className).getBytes(Charsets.UTF_8)
    )

    if (!implSourcePath.toFile.exists()) {
      // Now we generate the entity
      implSourcePath.getParent.toFile.mkdirs()
      Files.write(
        implSourcePath,
        source(
          service,
          packageName,
          implClassName,
          interfaceClassName
        ).getBytes(Charsets.UTF_8)
      )

      List(implSourcePath, interfaceSourcePath)
    } else {
      List(interfaceSourcePath)
    }
  }

  private[codegen] def source(
      service: ModelBuilder.ActionService,
      packageName: String,
      className: String,
      interfaceClassName: String
  ): String = {
    val messageTypes =
      service.commands.toSeq.flatMap(command => Seq(command.inputType, command.outputType))

    val imports = (messageTypes
      .filterNot(_.parent.javaPackage == packageName)
      .map(typeImport) ++ Seq(
      "com.akkaserverless.javasdk.action.*",
      "com.akkaserverless.javasdk.Reply"
    ) ++ (if (service.commands.exists(_.streamedOutput)) {
            Seq(
              "akka.stream.javadsl.Source",
              "akka.NotUsed"
            )
          } else Seq.empty)).distinct.sorted

    pretty(
      initialisedCodeComment <> line <> line <>
      "package" <+> packageName <> semi <> line <>
      line <>
      ssep(
        imports.to[immutable.Seq].map(pkg => "import" <+> pkg <> semi),
        line
      ) <> line <>
      line <>
      "/** An action. */" <> line <>
      "@Action" <> line <>
      `class`("public", s"$className extends $interfaceClassName") {
        ssep(
          service.commands.toSeq
            .map {
              command =>
                "@Override" <>
                line <>
                method(
                  "public",
                  maybeStreamed(
                    "Reply" <> angles(qualifiedType(command.outputType)),
                    streamed = command.streamedOutput
                  ),
                  lowerFirst(command.fqn.name),
                  List(
                    maybeStreamed(
                      qualifiedType(command.inputType),
                      streamed = command.streamedInput
                    ) <+> "event",
                    "ActionContext" <+> "ctx"
                  ),
                  emptyDoc
                ) {
                  "throw new RuntimeException" <> parens(
                    notImplementedError("command", command.fqn)
                  ) <> semi
                }
            }
            .to[immutable.Seq],
          line <> line
        )
      }
    ).layout
  }

  private[codegen] def interfaceSource(
      service: ModelBuilder.ActionService,
      packageName: String,
      className: String
  ): String = {
    val messageTypes =
      service.commands.toSeq.flatMap(command => Seq(command.inputType, command.outputType))

    val imports = (messageTypes
      .filterNot(_.parent.javaPackage == packageName)
      .map(typeImport) ++ Seq(
      "com.akkaserverless.javasdk.action.*",
      "com.akkaserverless.javasdk.Reply"
    ) ++ (if (service.commands.exists(cmd => cmd.streamedOutput || cmd.streamedInput)) {
            Seq(
              "akka.stream.javadsl.Source",
              "akka.NotUsed"
            )
          } else Seq.empty)).distinct.sorted

    pretty(
      managedCodeComment <> line <> line <>
      "package" <+> packageName <> semi <> line <>
      line <>
      ssep(
        imports.to[immutable.Seq].map(pkg => "import" <+> pkg <> semi),
        line
      ) <> line <>
      line <>
      "/** An action. */" <> line <>
      `class`("public abstract", "Abstract" + className) {
        ssep(
          service.commands.toSeq
            .map { command =>
              "@Handler" <>
              line <>
              abstractMethod(
                "public",
                maybeStreamed(
                  "Reply" <> angles(qualifiedType(command.outputType)),
                  streamed = command.streamedOutput
                ),
                lowerFirst(command.fqn.name),
                List(
                  maybeStreamed(
                    qualifiedType(command.inputType),
                    streamed = command.streamedInput
                  ) <+> "event",
                  "ActionContext" <+> "ctx"
                )
              ) <> semi
            }
            .to[immutable.Seq],
          line <> line
        )
      }
    ).layout
  }

  private def maybeStreamed(wrappedType: Doc, streamed: Boolean): Doc =
    if (streamed) {
      "Source" <> angles(
        wrappedType <> comma <+> "NotUsed"
      )
    } else wrappedType

}
