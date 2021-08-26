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
import com.lightbend.akkasls.codegen.java.EntityServiceSourceGenerator.generateImports
import org.bitbucket.inkytonik.kiama.output.PrettyPrinterTypes.Document

import _root_.java.nio.file.{Files, Path}
import scala.collection.immutable

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

    interfaceSourcePath.getParent.toFile.mkdirs()
    Files.write(
      interfaceSourcePath,
      interfaceSource(service, packageName, className).getBytes(Charsets.UTF_8)
    )

    if (!implSourcePath.toFile.exists()) {
      // Now we generate the view
      implSourcePath.getParent.toFile.mkdirs()
      Files.write(
        implSourcePath,
        viewSource(
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
  private[codegen] def viewHandler(service: ModelBuilder.EntityService,
                                          entity: ModelBuilder.ViewService,
                                          packageName: String,
                                          className: String): String = {

    val imports = generateImports(
      service.commands,
      None, // FIXME Some(entity.state), view state?
      packageName,
      otherImports = Seq(
        "import com.akkaserverless.javasdk.impl.view.ViewException",
        "import com.akkaserverless.javasdk.impl.view.ViewHandler",
        "import com.akkaserverless.javasdk.view.UpdateContext",
        "import com.akkaserverless.javasdk.view.View",
        "import scala.Option"
      )
    )

    val serviceApiOuterClass = service.fqn.parent.javaOuterClassname
    val outerClassAndState = ??? // s"${entity.fqn.parent.javaOuterClassname}.${entity.state.fqn.name}"
    val cases = service.commands
      .map { cmd =>
        val methodName = cmd.fqn.name
        val inputType = s"$serviceApiOuterClass.${cmd.inputType.name}"
        s"""|case "$methodName":
            |  return view().${lowerFirst(methodName)}(
            |      state,
            |      (${inputType}) event);
            |""".stripMargin
      }

    s"""|$managedCodeCommentString
        |package $packageName;
        |
        |$imports
        |
        |/** A value entity handler */
        |public class ${className}Handler extends ViewHandler<$outerClassAndState, ${className}> {
        |
        |  public ${className}Handler(${className} view) {
        |    super(view);
        |  }
        |
        |  public ${className}Handler(${className} entity) {
        |    this.entity = entity;
        |  }
        |
        |  @Override
        |  public View.UpdateEffect<$outerClassAndState> handleUpdate(
        |      String eventName,
        |      $outerClassAndState state,
        |      Object event,
        |      UpdateContext context) {
        |
        |    switch (eventName) {
        |      ${Syntax.indent(cases, 6)}
        |
        |      default:
        |        throw new ViewException(
        |            context.viewId(),
        |            eventName,
        |            "No command handler found for command ["
        |                + eventName
        |                + "] on "
        |                + view().getClass().toString(),
        |            Option.empty());
        |    }
        |
        |}""".stripMargin
  }


  private[codegen] def viewSource(
      service: ModelBuilder.ViewService,
      packageName: String,
      className: String,
      interfaceClassName: String
  ): String = {
    val messageTypes =
      service.transformedUpdates.toSeq.flatMap(command => Seq(command.inputType, command.outputType))

    val imports = (messageTypes
      .filterNot(_.parent.javaPackage == packageName)
      .map(typeImport) ++ immutable.Seq(
      "com.akkaserverless.javasdk.view.View",
      "java.util.Optional"
    )).distinct.sorted.to[immutable.Seq]

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
          service.transformedUpdates.toSeq
            .map { update =>
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
            }
            .to[immutable.Seq],
          line <> line
        )
      }
    ).layout
  }

  private[codegen] def interfaceSource(
      service: ModelBuilder.ViewService,
      packageName: String,
      className: String
  ): String = {
    val messageTypes =
      service.transformedUpdates.toSeq.flatMap(command => immutable.Seq(command.inputType, command.outputType))

    val imports = (messageTypes
      .filterNot(_.parent.javaPackage == packageName)
      .map(typeImport) ++
    Seq(
      "com.akkaserverless.javasdk.view.*",
      "java.util.Optional"
    )).distinct
      .to[immutable.Seq]
      .sorted

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
          immutable
            .Seq(
              service.transformedUpdates.toSeq
                .map { update =>
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
                }
            )
            .flatten,
          line <> line
        )
      }
    ).layout
  }

}
