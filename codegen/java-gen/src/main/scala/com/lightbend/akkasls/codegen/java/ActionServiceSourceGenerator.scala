/*
 * Copyright (c) Lightbend Inc. 2021
 *
 */

package com.lightbend.akkasls.codegen
package java

import com.google.common.base.Charsets
import org.bitbucket.inkytonik.kiama.output.PrettyPrinterTypes.Document

import _root_.java.nio.file.{ Files, Path }

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
    val className   = service.fqn.name
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
      service: ModelBuilder.ActionService,
      packageName: String,
      className: String,
      interfaceClassName: String
  ): Document = {
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
      "package" <+> packageName <> semi <> line <>
      line <>
      ssep(
        imports.map(pkg => "import" <+> pkg <> semi),
        line
      ) <> line <>
      line <>
      "/** An action. */" <> line <>
      "@Action" <> line <>
      `class`("public", s"$className extends $interfaceClassName") {
        ssep(
          service.commands.toSeq.map { command =>
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
          },
          line <> line
        )
      }
    )
  }

  private[codegen] def interfaceSource(
      service: ModelBuilder.ActionService,
      packageName: String,
      className: String
  ): Document = {
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
      "package" <+> packageName <> semi <> line <>
      line <>
      ssep(
        imports.map(pkg => "import" <+> pkg <> semi),
        line
      ) <> line <>
      line <>
      "/** An action. */" <> line <>
      `class`("public abstract", className + "Interface") {
        ssep(
          service.commands.toSeq.map { command =>
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
          },
          line <> line
        )
      }
    )
  }

  private def maybeStreamed(wrappedType: Doc, streamed: Boolean): Doc =
    if (streamed) {
      "Source" <> angles(
        wrappedType <> comma <+> "NotUsed"
      )
    } else wrappedType

}
