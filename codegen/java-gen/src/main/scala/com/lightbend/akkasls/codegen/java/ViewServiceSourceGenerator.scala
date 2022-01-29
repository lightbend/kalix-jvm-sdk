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

/**
 * Responsible for generating Java sources for a view
 */
object ViewServiceSourceGenerator {
  import com.lightbend.akkasls.codegen.SourceGeneratorUtils._

  /**
   * Generate Java sources for provider, handler, abstract baseclass for a view, and also the user view source file if
   * it does not already exist.
   */
  def generate(service: ModelBuilder.ViewService): GeneratedFiles = {
    val pkg = service.fqn.parent

    GeneratedFiles.Empty
      .addManaged(File.java(pkg, service.abstractViewName, abstractView(service, pkg)))
      .addManaged(File.java(pkg, service.routerName, viewRouter(service, pkg)))
      .addManaged(File.java(pkg, service.providerName, viewProvider(service, pkg)))
      .addUnmanaged(File.java(pkg, service.className, viewSource(service, pkg)))
  }

  private[codegen] def viewRouter(view: ModelBuilder.ViewService, packageName: PackageNaming): String = {
    import Types.View._
    val cases = view.transformedUpdates
      .map { cmd =>
        val methodName = cmd.name
        val inputType = cmd.inputType
        c"""|case "$methodName":
            |  return view().${lowerFirst(methodName)}(
            |      state,
            |      (${inputType}) event);
            |"""
      }

    JavaGeneratorUtils.generate(
      packageName,
      c"""|$managedComment
          |
          |public class ${view.routerName} extends $ViewRouter<${view.state.fqn}, ${view.impl}> {
          |
          |  public ${view.routerName}(${view.impl} view) {
          |    super(view);
          |  }
          |
          |  @Override
          |  public $View.UpdateEffect<${view.state.fqn}> handleUpdate(
          |      String eventName,
          |      ${view.state.fqn} state,
          |      Object event) {
          |
          |    switch (eventName) {
          |      $cases
          |      default:
          |        throw new $UpdateHandlerNotFound(eventName);
          |    }
          |  }
          |
          |}
          |""",
      Nil)
  }

  private[codegen] def viewProvider(view: ModelBuilder.ViewService, packageName: PackageNaming): String = {
    import Types.View._
    import Types._

    JavaGeneratorUtils.generate(
      packageName,
      c"""|$managedComment
          |
          |public class ${view.providerName} implements $ViewProvider<${view.state.fqn}, ${view.impl}> {
          |
          |  private final $Function<$ViewCreationContext, ${view.className}> viewFactory;
          |  private final String viewId;
          |  private final $ViewOptions options;
          |
          |  /** Factory method of ${view.impl} */
          |  public static ${view.providerName} of(
          |      $Function<$ViewCreationContext, ${view.impl}> viewFactory) {
          |    return new ${view.providerName}(viewFactory, "${view.viewId}", $ViewOptions.defaults());
          |  }
          |
          |  private ${view.providerName}(
          |      $Function<$ViewCreationContext, ${view.impl}> viewFactory,
          |      String viewId,
          |      $ViewOptions options) {
          |    this.viewFactory = viewFactory;
          |    this.viewId = viewId;
          |    this.options = options;
          |  }
          |
          |  @Override
          |  public String viewId() {
          |    return viewId;
          |  }
          |
          |  @Override
          |  public final $ViewOptions options() {
          |    return options;
          |  }
          |
          |  public final ${view.providerName} withOptions($ViewOptions options) {
          |    return new ${view.providerName}(viewFactory, viewId, options);
          |  }
          |
          |  /**
          |   * Use a custom view identifier. By default, the viewId is the same as the proto service name.
          |   * A different identifier can be needed when making rolling updates with changes to the view definition.
          |   */
          |  public ${view.providerName} withViewId(String viewId) {
          |    return new ${view.providerName}(viewFactory, viewId, options);
          |  }
          |
          |  @Override
          |  public final $Descriptors.ServiceDescriptor serviceDescriptor() {
          |    return ${view.fqn.descriptorImport}.getDescriptor().findServiceByName("${view.fqn.protoName}");
          |  }
          |
          |  @Override
          |  public final ${view.routerName} newRouter($ViewCreationContext context) {
          |    return new ${view.routerName}(viewFactory.apply(context));
          |  }
          |
          |  @Override
          |  public final $Descriptors.FileDescriptor[] additionalDescriptors() {
          |    return new $Descriptors.FileDescriptor[] {${view.fqn.parent.javaOuterClassname}.getDescriptor()};
          |  }
          |}
          |""",
      Nil)
  }

  private[codegen] def viewSource(view: ModelBuilder.ViewService, packageName: PackageNaming): String = {
    import Types.View._

    val emptyState =
      if (view.transformedUpdates.isEmpty)
        c""
      else
        c"""|
            |@Override
            |public ${view.state.fqn} emptyState() {
            |  throw new UnsupportedOperationException("Not implemented yet, replace with your empty view state");
            |}
            |"""

    val handlers = view.transformedUpdates.map { update =>
      val stateType = update.outputType
      c"""@Override
         |public $View.UpdateEffect<${stateType}> ${lowerFirst(update.name)}(
         |  $stateType state, ${update.inputType} ${lowerFirst(update.inputType.name)}) {
         |  throw new UnsupportedOperationException("Update handler for '${update.name}' not implemented yet");
         |}"""
    }

    JavaGeneratorUtils.generate(
      packageName,
      c"""${unmanagedComment(Left(view))}
       |
       |public class ${view.className} extends ${view.abstractView} {
       |
       |  public ${view.className}($ViewContext context) {}
       |  $emptyState
       |  $handlers
       |}
       |""",
      Nil)
  }

  private[codegen] def abstractView(view: ModelBuilder.ViewService, packageName: PackageNaming): String = {
    import Types.View._

    val emptyState =
      if (view.transformedUpdates.isEmpty)
        c"""|
            |@Override
            |public ${view.state.fqn} emptyState() {
            |  return null; // emptyState is only used with transform_updates=true
            |}
            |"""
      else
        c""

    val handlers = view.transformedUpdates.map { update =>
      val stateType = update.outputType
      c"""public abstract $View.UpdateEffect<$stateType> ${lowerFirst(update.name)}(
         |  $stateType state, ${update.inputType} ${lowerFirst(update.inputType.name)});"""

    }

    JavaGeneratorUtils.generate(
      packageName,
      c"""$managedComment
         |
         |public abstract class ${view.abstractViewName} extends $View<${view.state.fqn}> {
         |  $emptyState
         |  $handlers
         |}
         |""",
      Nil)
  }

}
