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

import _root_.java.nio.file.{ Files, Path }
import scala.collection.immutable

/**
 * Responsible for generating Java sources for a view
 */
object ViewServiceSourceGenerator {
  import SourceGenerator._

  /**
   * Generate Java sources for provider, handler, abstract baseclass for a view, and also the user view source file if
   * it does not already exist.
   *
   * Impure.
   */
  def generate(
      service: ModelBuilder.ViewService,
      sourceDirectory: Path,
      testSourceDirectory: Path,
      integrationTestSourceDirectory: Path,
      generatedSourceDirectory: Path): Iterable[Path] = {
    // Note that we generate all sources also with no transformations - no actual logic operations to make
    // adding such later minimal fuss in the user code
    val generatedSources = Seq.newBuilder[Path]

    val packageName = service.fqn.parent.javaPackage
    val packagePath = packageAsPath(packageName)

    val implSourcePath =
      sourceDirectory.resolve(packagePath.resolve(service.viewClassName + ".java"))

    val abstractViewPath =
      generatedSourceDirectory.resolve(packagePath.resolve(service.abstractViewName + ".java"))
    abstractViewPath.getParent.toFile.mkdirs()
    Files.write(abstractViewPath, abstractView(service, packageName).getBytes(Charsets.UTF_8))
    generatedSources += abstractViewPath

    // Only if there is no user view code already present
    if (!implSourcePath.toFile.exists()) {
      implSourcePath.getParent.toFile.mkdirs()
      Files.write(implSourcePath, viewSource(service, packageName).getBytes(Charsets.UTF_8))
      generatedSources += implSourcePath
    }

    val handlerSourcePath = generatedSourceDirectory.resolve(packagePath.resolve(service.handlerName + ".java"))
    Files.write(handlerSourcePath, viewHandler(service, packageName).getBytes(Charsets.UTF_8))
    generatedSources += handlerSourcePath

    val providerSourcePath = generatedSourceDirectory.resolve(packagePath.resolve(service.providerName + ".java"))
    Files.write(providerSourcePath, viewProvider(service, packageName).getBytes(Charsets.UTF_8))
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
        "com.akkaserverless.javasdk.view.View"))

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
        |public class ${view.handlerName} extends ViewHandler<${qualifiedType(view.state.fqn)}, ${view.viewClassName}> {
        |
        |  public ${view.handlerName}(${view.viewClassName} view) {
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
      view.state,
      packageName,
      otherImports = Seq(
        "com.akkaserverless.javasdk.impl.view.UpdateHandlerNotFound",
        "com.akkaserverless.javasdk.impl.view.ViewHandler",
        "com.akkaserverless.javasdk.view.ViewProvider",
        "com.akkaserverless.javasdk.view.ViewCreationContext",
        "com.akkaserverless.javasdk.view.View",
        view.classNameQualified,
        "com.google.protobuf.Descriptors",
        "com.google.protobuf.EmptyProto",
        "java.util.function.Function"))

    s"""|$managedCodeCommentString
        |package $packageName;
        |
        |$imports
        |
        |public class ${view.providerName} implements ViewProvider<${qualifiedType(view.state.fqn)}, ${view.viewClassName}> {
        |
        |  private final Function<ViewCreationContext, ${view.viewClassName}> viewFactory;
        |  private final String viewId;
        |
        |  /** Factory method of ${view.viewClassName} */
        |  public static ${view.providerName} of(
        |      Function<ViewCreationContext, ${view.viewClassName}> viewFactory) {
        |    return new ${view.providerName}(viewFactory, "${view.viewId}");
        |  }
        |
        |  private ${view.providerName}(
        |      Function<ViewCreationContext, ${view.viewClassName}> viewFactory,
        |      String viewId) {
        |    this.viewFactory = viewFactory;
        |    this.viewId = viewId;
        |  }
        |
        |  @Override
        |  public String viewId() {
        |    return viewId;
        |  }
        |
        |  /**
        |   * Use a custom view identifier. By default, the viewId is the same as the proto service name.
        |   * A different identifier can be needed when making rolling updates with changes to the view definition.
        |   */
        |  public ${view.providerName} withViewId(String viewId) {
        |    return new ${view.providerName}(viewFactory, viewId);
        |  }
        |
        |  @Override
        |  public final Descriptors.ServiceDescriptor serviceDescriptor() {
        |    return ${view.fqn.parent.javaOuterClassname}.getDescriptor().findServiceByName("${view.fqn.protoName}");
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

  private[codegen] def viewSource(view: ModelBuilder.ViewService, packageName: String): String = {

    val imports = generateImports(view.commandTypes, packageName, Seq("com.akkaserverless.javasdk.view.ViewContext"))

    val emptyState =
      if (view.transformedUpdates.isEmpty)
        ""
      else
        s"""|
            |  @Override
            |  public ${qualifiedType(view.state.fqn)} emptyState() {
            |    throw new UnsupportedOperationException("Not implemented yet, replace with your empty view state");
            |  }
            |""".stripMargin

    val handlers = view.transformedUpdates.map { update =>
      val stateType = qualifiedType(update.outputType)
      s"""@Override
         |public UpdateEffect<${stateType}> ${lowerFirst(update.fqn.name)}(
         |  $stateType state, ${qualifiedType(update.inputType)} ${lowerFirst(update.inputType.name)}) {
         |  throw new UnsupportedOperationException("Update handler for '${update.fqn.name}' not implemented yet");
         |}""".stripMargin
    }

    s"""$generatedCodeCommentString
       |package $packageName;
       |
       |$imports
       |
       |public class ${view.viewClassName} extends ${view.abstractViewName} {
       |
       |  public ${view.viewClassName}(ViewContext context) {}
       |$emptyState
       |  ${Syntax.indent(handlers, 2)}
       |}""".stripMargin
  }

  private[codegen] def abstractView(view: ModelBuilder.ViewService, packageName: String): String = {
    val imports = generateImports(view.commandTypes, packageName, Seq("com.akkaserverless.javasdk.view.View"))

    val emptyState =
      if (view.transformedUpdates.isEmpty)
        s"""|
            |  @Override
            |  public ${qualifiedType(view.state.fqn)} emptyState() {
            |    return null; // emptyState is only used with transform_updates=true
            |  }
            |""".stripMargin
      else
        ""

    val handlers = view.transformedUpdates.map { update =>
      val stateType = qualifiedType(update.outputType)
      s"""public abstract UpdateEffect<${stateType}> ${lowerFirst(update.fqn.name)}(
         |  $stateType state, ${qualifiedType(update.inputType)} ${lowerFirst(update.inputType.name)});""".stripMargin

    }

    s"""$managedCodeCommentString
      |package $packageName;
      |
      |$imports
      |
      |public abstract class ${view.abstractViewName} extends View<${qualifiedType(view.state.fqn)}> {
      |$emptyState
      |  ${Syntax.indent(handlers, 2)}
      |}""".stripMargin
  }

}
