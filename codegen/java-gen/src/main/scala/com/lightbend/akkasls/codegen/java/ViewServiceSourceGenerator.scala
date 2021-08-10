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
import org.bitbucket.inkytonik.kiama.output.PrettyPrinterTypes.Document

import _root_.java.nio.file.{Files, Path}

/**
 * Responsible for generating Java source from an entity model
 */
object ViewServiceSourceGenerator {
  import SourceGenerator._

  /**
   * Generate Java source from views where the target source and test source directories have no existing source.
   *
   * Also generates a main source file if it does not already exist.
   *
   * Impure.
   */
  def generate(
      service: ModelBuilder.ViewService,
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

    val _ = interfaceSourcePath.getParent.toFile.mkdirs()
    val _ = Files.write(
      interfaceSourcePath,
      interfaceSource(service, packageName, className).layout.getBytes(
        Charsets.UTF_8
      )
    )

    if (!implSourcePath.toFile.exists()) {
      // Now we generate the entity
      val _ = implSourcePath.getParent.toFile.mkdirs()
      val _ = Files.write(
        implSourcePath,
        source(
          service,
          packageName,
          implClassName,
          interfaceClassName
        ).layout.getBytes(
          Charsets.UTF_8
        )
      )

      List(implSourcePath, interfaceSourcePath)
    } else {
      List(interfaceSourcePath)
    }
  }

  private[codegen] def source(
      service: ModelBuilder.ViewService,
      packageName: String,
      className: String,
      interfaceClassName: String
  ): Document = {
    val messageTypes =
      service.transformedUpdates.toSeq.flatMap(command => Seq(command.inputType, command.outputType))

    val imports = (messageTypes
      .filterNot(_.parent.javaPackage == packageName)
      .map(typeImport) ++ Seq(
      "com.akkaserverless.javasdk.view.View",
      "java.util.Optional"
    )).distinct.sorted

    pretty(
      initialisedCodeComment <> line <> line <>
      "package" <+> packageName <> semi <> line <>
      line <>
      ssep(
        imports.map(pkg => "import" <+> pkg <> semi),
        line
      ) <> line <>
      line <>
      "/** A view. */" <> line <>
      "@View" <> line <>
      `class`("public", s"$className extends $interfaceClassName") {
        ssep(
          service.transformedUpdates.toSeq.map { update =>
            "@Override" <>
            line <>
            method(
              "public",
              qualifiedType(update.outputType),
              lowerFirst(update.fqn.name),
              List(
                qualifiedType(update.inputType) <+> "event",
                "Optional" <> angles(
                  qualifiedType(update.outputType)
                ) <+> "state"
              ),
              emptyDoc
            ) {
              "throw new RuntimeException" <> parens(
                notImplementedError("update", update.fqn)
              ) <> semi
            }
          },
          line <> line
        )
      }
    )
  }

  private[codegen] def interfaceSource(
      service: ModelBuilder.ViewService,
      packageName: String,
      className: String
  ): Document = {
    val messageTypes =
      service.transformedUpdates.toSeq.flatMap(command => Seq(command.inputType, command.outputType))

    val imports = (messageTypes
      .filterNot(_.parent.javaPackage == packageName)
      .map(typeImport) ++
    Seq(
      "com.akkaserverless.javasdk.view.*",
      "java.util.Optional"
    )).distinct.sorted

    pretty(
      managedCodeComment <> line <> line <>
      "package" <+> packageName <> semi <> line <>
      line <>
      ssep(
        imports.map(pkg => "import" <+> pkg <> semi),
        line
      ) <> line <>
      line <>
      "/** A view. */" <> line <>
      `class`("public abstract", "Abstract" + className) {
        ssep(
          service.transformedUpdates.toSeq.map { update =>
            "@UpdateHandler" <>
            line <>
            abstractMethod(
              "public",
              qualifiedType(update.outputType),
              lowerFirst(update.fqn.name),
              List(
                qualifiedType(update.inputType) <+> "event",
                "Optional" <> angles(
                  qualifiedType(update.outputType)
                ) <+> "state"
              )
            ) <> semi
          },
          line <> line
        )
      }
    )
  }

}
