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

    val generatedSources = Seq.newBuilder[Path]

    val packageName = service.fqn.parent.javaPackage
    val className = service.fqn.name
    val packagePath = packageAsPath(packageName)

    val implClassName = className
    val implSourcePath =
      sourceDirectory.resolve(packagePath.resolve(implClassName + ".java"))

    val interfaceSourcePath =
      generatedSourceDirectory.resolve(packagePath.resolve(service.interfaceName + ".java"))
    interfaceSourcePath.getParent.toFile.mkdirs()
    Files.write(
      interfaceSourcePath,
      interfaceSource(service, packageName).getBytes(Charsets.UTF_8)
    )
    generatedSources += interfaceSourcePath

    // Only if there is no user view code already present
    if (!implSourcePath.toFile.exists()) {
      implSourcePath.getParent.toFile.mkdirs()
      Files.write(
        implSourcePath,
        viewSource(service, packageName).getBytes(Charsets.UTF_8)
      )
      generatedSources += implSourcePath
    }

    val handlerSourcePath = generatedSourceDirectory.resolve(packagePath.resolve(service.handlerName + ".java"))
    Files.write(
      handlerSourcePath,
      viewHandler(service, packageName).getBytes(Charsets.UTF_8)
    )
    generatedSources += handlerSourcePath

    val providerSourcePath = generatedSourceDirectory.resolve(packagePath.resolve(service.providerName + ".java"))
    Files.write(
      providerSourcePath,
      viewProvider(service, packageName).getBytes(Charsets.UTF_8)
    )
    generatedSources += providerSourcePath

    generatedSources.result()
  }
  private[codegen] def viewHandler(view: ModelBuilder.ViewService, packageName: String): String = {

    val imports = generateImports(
      view.commands,
      view.state,
      packageName,
      otherImports = Seq(
        "com.akkaserverless.javasdk.impl.view.UpdateHandlerNotFound",
        "com.akkaserverless.javasdk.impl.view.ViewHandler",
        "com.akkaserverless.javasdk.view.View"
      )
    )

    val serviceApiOuterClass = view.fqn.parent.javaOuterClassname
    val cases = view.transformedUpdates
      .map { cmd =>
        val methodName = cmd.fqn.name
        val inputType = qualifiedType(cmd.inputType)
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
        |/** A view handler */
        |public class ${view.handlerName} extends ViewHandler<${qualifiedType(view.state.fqn)}, ${view.className}> {
        |
        |  public ${view.handlerName}(${view.className} view) {
        |    super(view);
        |  }
        |
        |  @Override
        |  public View.UpdateEffect<${qualifiedType(view.state.fqn)}> handleUpdate(
        |      String eventName,
        |      ${qualifiedType(view.state.fqn)} state,
        |      Object event) {
        |
        |    switch (eventName) {
        |      ${Syntax.indent(cases, 6)}
        |
        |      default:
        |        throw new UpdateHandlerNotFound(eventName);
        |    }
        |  }
        |
        |}""".stripMargin
  }

  private[codegen] def viewProvider(view: ModelBuilder.ViewService, packageName: String): String = {
    val imports = generateImports(
      Nil,
      packageName,
      Seq(
        "com.akkaserverless.javasdk.impl.view.UpdateHandlerNotFound",
        "com.akkaserverless.javasdk.impl.view.ViewHandler",
        "com.akkaserverless.javasdk.view.ViewProvider",
        "com.akkaserverless.javasdk.view.ViewCreationContext",
        "com.akkaserverless.javasdk.view.View",
        "com.akkaserverless.javasdk.valueentity.ValueEntityProvider",
        view.fqn.fullQualifiedName,
        "com.google.protobuf.Descriptors",
        "com.google.protobuf.EmptyProto",
        "java.util.function.Function"
      )
    )

    s"""|$managedCodeCommentString
        |package $packageName;
        |
        |$imports
        |
        |public class ${view.providerName} implements ViewProvider {
        |
        |  private final Function<ViewCreationContext, ${view.className}> viewFactory;
        |
        |  /** Factory method of ${view.className} */
        |  public static ${view.providerName} of(
        |      Function<ViewCreationContext, ${view.className}> viewFactory) {
        |    return new ${view.providerName}(viewFactory);
        |  }
        |
        |  private ${view.providerName}(
        |      Function<ViewCreationContext, ${view.className}> viewFactory) {
        |    this.viewFactory = viewFactory;
        |  }
        |
        |  @Override
        |  public String viewId() {
        |    return "${view.viewId}";
        |  }
        |
        |  @Override
        |  public final Descriptors.ServiceDescriptor serviceDescriptor() {
        |    return ${view.fqn.parent.javaOuterClassname}.getDescriptor().findServiceByName("${view.fqn.name}");
        |  }
        |
        |  @Override
        |  public final ${view.handlerName} newHandler(ViewCreationContext context) {
        |    return new ${view.handlerName}(viewFactory.apply(context));
        |  }
        |
        |  @Override
        |  public final Descriptors.FileDescriptor[] additionalDescriptors() {
        |    return new Descriptors.FileDescriptor[] {${view.fqn.parent.javaOuterClassname}.getDescriptor()};
        |  }
        |}""".stripMargin
  }

  private[codegen] def viewSource(
      view: ModelBuilder.ViewService,
      packageName: String
  ): String = {

    val imports = generateImports(
      view.commandTypes,
      packageName,
      Seq(
        "com.akkaserverless.javasdk.view.View",
        "com.akkaserverless.javasdk.view.ViewContext",
        "java.util.function.Function"
      )
    )

    val handlers = view.transformedUpdates.map { update =>
      val stateType = qualifiedType(update.outputType)
      s"""@Override
         |public UpdateEffect<${stateType}> ${lowerFirst(update.fqn.name)}(
         |  $stateType state, ${qualifiedType(update.inputType)} ${lowerFirst(update.fqn.name)}) {
         |  throw new RuntimeException("Update handler for '${update.fqn.name}' not implemented yet");
         |}""".stripMargin
    }

    s"""$managedCodeCommentString
       |package $packageName;
       |
       |$imports
       |
       |public class ${view.className} extends ${view.interfaceName} {
       |
       |  public ${view.className}(ViewContext context) {}
       |
       |  @Override
       |  public ${qualifiedType(view.state.fqn)} emptyState() {
       |    throw new RuntimeException("Empty state for '${view.className}' not implemented yet");
       |  }
       |
       |  ${Syntax.indent(handlers, 2)}
       |}""".stripMargin
  }

  private[codegen] def interfaceSource(
      view: ModelBuilder.ViewService,
      packageName: String
  ): String = {
    val imports = generateImports(
      view.commandTypes,
      packageName,
      Seq(
        "com.akkaserverless.javasdk.view.View",
        "java.util.function.Function"
      )
    )

    val handlers = view.transformedUpdates.map { update =>
      val stateType = qualifiedType(update.outputType)
      s"""public abstract UpdateEffect<${stateType}> ${lowerFirst(update.fqn.name)}(
         |  $stateType state, ${qualifiedType(update.inputType)} ${lowerFirst(update.fqn.name)});""".stripMargin

    }

    s"""$managedCodeCommentString
      |package $packageName;
      |
      |$imports
      |
      |public abstract class ${view.interfaceName} extends View<${qualifiedType(view.state.fqn)}> {
      |
      |  ${Syntax.indent(handlers, 2)}
      |}""".stripMargin
  }

}
