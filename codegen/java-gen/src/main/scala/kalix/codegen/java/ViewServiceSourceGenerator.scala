/*
 * Copyright 2024 Lightbend Inc.
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

package kalix.codegen
package java

/**
 * Responsible for generating Java sources for a view
 */
object ViewServiceSourceGenerator {
  import kalix.codegen.SourceGeneratorUtils._

  /**
   * Generate Java sources for provider, handler, abstract baseclass for a view, and also the user view source file if
   * it does not already exist.
   */
  def generate(service: ModelBuilder.ViewService): GeneratedFiles = {
    val pkg = service.messageType.parent

    GeneratedFiles.Empty
      .addManaged(File.java(pkg, service.abstractViewName, abstractView(service, pkg)))
      .addManaged(File.java(pkg, service.routerName, viewRouter(service, pkg)))
      .addManaged(File.java(pkg, service.providerName, viewProvider(service, pkg)))
      .addUnmanaged(File.java(pkg, service.className, viewSource(service, pkg)))
  }

  private[codegen] def viewRouter(view: ModelBuilder.ViewService, packageName: PackageNaming): String = {
    JavaGeneratorUtils.generate(
      packageName,
      c"""|$managedComment
          |
          |${viewRouterContent(view)}
          |""",
      Nil)
  }

  private[codegen] def viewRouterContent(view: ModelBuilder.ViewService): CodeBlock = {
    if (view.tables.size > 1) viewRouterMultiTable(view)
    else viewRouterClass(view.routerName, view.className, view.state.messageType, view.transformedUpdates)
  }

  private[codegen] def viewRouterClass(
      className: String,
      implClassName: String,
      stateType: MessageType,
      transformedUpdates: Iterable[ModelBuilder.Command],
      static: Boolean = false): CodeBlock = {
    import Types.View._
    val cases = transformedUpdates
      .map { cmd =>
        val methodName = cmd.name
        val inputType = cmd.inputType
        if (cmd.handleDeletes) {
          c"""|case "$methodName":
              |  return view().${lowerFirst(methodName)}(state);
              |"""
        } else {
          c"""|case "$methodName":
              |  return view().${lowerFirst(methodName)}(
              |      state,
              |      ($inputType) event);
              |"""
        }
      }

    val modifiers = if (static) "public static" else "public"

    c"""|$modifiers class $className extends $ViewRouter<$stateType, $implClassName> {
        |
        |  public $className($implClassName view) {
        |    super(view);
        |  }
        |
        |  @Override
        |  public $View.UpdateEffect<$stateType> handleUpdate(
        |      String eventName,
        |      $stateType state,
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
        |"""
  }

  private[codegen] def viewRouterMultiTable(view: ModelBuilder.ViewService): CodeBlock = {
    import Types.View._
    val routerClasses = view.tables.flatMap { table =>
      val transformedUpdates = view.tableTransformedUpdates(table)
      if (transformedUpdates.isEmpty) None
      else {
        val className = view.tableClassName(table)
        val stateType = view.tableType(table)
        Some(
          viewRouterClass(
            s"${className}Router",
            s"${view.abstractViewName}.Abstract$className",
            stateType,
            transformedUpdates,
            static = true))
      }
    }
    val routerVariables = view.tables.flatMap { table =>
      if (view.tableTransformedUpdates(table).isEmpty) None
      else {
        val className = view.tableClassName(table)
        Some(c"""private ${className}Router ${lowerFirst(className)}Router;""")
      }
    }
    val routerInits = view.tables.flatMap { table =>
      if (view.tableTransformedUpdates(table).isEmpty) None
      else {
        val className = view.tableClassName(table)
        Some(c"""${lowerFirst(className)}Router = new ${className}Router(view.${lowerFirst(className)}());""")
      }
    }
    val routerCases = view.transformedUpdates.map { update =>
      val className = view.tableClassName(update.viewTable)
      c"""|case "${update.name}":
          |  return ${lowerFirst(className)}Router;
          |"""
    }
    c"""|public class ${view.routerName} extends $ViewMultiTableRouter {
        |
        |  $routerVariables
        |
        |  public ${view.routerName}(${view.impl} view) {
        |    $routerInits
        |  }
        |
        |  $routerClasses
        |
        |  @Override
        |  public $ViewRouter<?, ?> viewRouter(String eventName) {
        |    switch (eventName) {
        |      $routerCases
        |      default:
        |        throw new $UpdateHandlerNotFound(eventName);
        |    }
        |  }
        |
        |}
        |"""
  }

  private[codegen] def viewProvider(view: ModelBuilder.ViewService, packageName: PackageNaming): String = {
    import Types.View._
    import Types._

    JavaGeneratorUtils.generate(
      packageName,
      c"""|$managedComment
          |
          |public class ${view.providerName} implements $ViewProvider {
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
          |    return ${view.messageType.descriptorImport}.getDescriptor().findServiceByName("${view.messageType.protoName}");
          |  }
          |
          |  @Override
          |  public final ${view.routerName} newRouter($ViewCreationContext context) {
          |    return new ${view.routerName}(viewFactory.apply(context));
          |  }
          |
          |  @Override
          |  public final $Descriptors.FileDescriptor[] additionalDescriptors() {
          |    return new $Descriptors.FileDescriptor[] {${view.messageType.parent.javaOuterClassname}.getDescriptor()};
          |  }
          |}
          |""",
      Nil)
  }

  private[codegen] def viewSource(view: ModelBuilder.ViewService, packageName: PackageNaming): String = {
    JavaGeneratorUtils.generate(
      packageName,
      c"""|${unmanagedComment(Left(view))}
          |
          |${viewSourceContent(view)}
          |""",
      Nil)
  }

  private[codegen] def viewSourceContent(view: ModelBuilder.ViewService): CodeBlock = {
    if (view.tables.size > 1) viewSourceMultiTable(view)
    else viewSourceClass(view.className, view.abstractViewName, view.state.messageType, view.transformedUpdates)
  }

  private[codegen] def viewSourceClass(
      className: String,
      abstractClassName: String,
      stateType: MessageType,
      transformedUpdates: Iterable[ModelBuilder.Command],
      static: Boolean = false): CodeBlock = {
    import Types.View._

    val methods =
      if (transformedUpdates.isEmpty) Seq.empty
      else {
        val emptyState =
          c"""|@Override
              |public $stateType emptyState() {
              |  throw new UnsupportedOperationException("Not implemented yet, replace with your empty view state");
              |}
              |"""

        val updateHandlers = transformedUpdates.toSeq.map { update =>
          val stateType = update.outputType
          if (update.handleDeletes) {
            c"""|@Override
                |public $View.UpdateEffect<$stateType> ${lowerFirst(update.name)}(
                |    $stateType state) {
                |  throw new UnsupportedOperationException("Delete handler for '${update.name}' not implemented yet");
                |}
                |"""
          } else {
            c"""|@Override
                |public $View.UpdateEffect<$stateType> ${lowerFirst(update.name)}(
                |    $stateType state,
                |    ${update.inputType} ${lowerFirst(update.inputType.name)}) {
                |  throw new UnsupportedOperationException("Update handler for '${update.name}' not implemented yet");
                |}
                |"""
          }
        }

        emptyState +: updateHandlers
      }

    val modifiers = if (static) "public static" else "public"

    c"""|$modifiers class $className extends $abstractClassName {
        |
        |  public $className($ViewContext context) {}
        |
        |  $methods
        |}"""
  }

  private[codegen] def viewSourceMultiTable(view: ModelBuilder.ViewService): CodeBlock = {
    import Types.View._

    val viewTables = view.tables.flatMap { table =>
      val transformedUpdates = view.tableTransformedUpdates(table)
      if (transformedUpdates.isEmpty) None
      else {
        val className = view.tableClassName(table)
        val stateType = view.tableType(table)
        Some(c"""|@Override
                 |public $className create$className($ViewContext context) {
                 |  return new $className(context);
                 |}
                 |
                 |${viewSourceClass(className, s"Abstract$className", stateType, transformedUpdates, static = true)}
                 |""")
      }
    }

    c"""|public class ${view.className} extends ${view.abstractViewName} {
        |
        |  public ${view.className}($ViewContext context) {
        |    super(context);
        |  }
        |
        |  $viewTables
        |}"""
  }

  private[codegen] def abstractView(view: ModelBuilder.ViewService, packageName: PackageNaming): String = {
    JavaGeneratorUtils.generate(
      packageName,
      c"""|$managedComment
          |
          |${abstractViewContent(view)}
          |""",
      Nil)
  }

  private[codegen] def abstractViewContent(view: ModelBuilder.ViewService): CodeBlock = {
    if (view.tables.size > 1) abstractViewMultiTable(view)
    else abstractViewClass(view.abstractViewName, view.state.messageType, view.transformedUpdates)
  }

  private[codegen] def abstractViewClass(
      className: String,
      stateType: MessageType,
      transformedUpdates: Iterable[ModelBuilder.Command],
      static: Boolean = false): CodeBlock = {
    import Types.View._

    val handlers = transformedUpdates.map { update =>
      val stateType = update.outputType
      if (update.handleDeletes) {
        c"""|public abstract $View.UpdateEffect<$stateType> ${lowerFirst(update.name)}(
            |    $stateType state);
            |"""
      } else {
        c"""|public abstract $View.UpdateEffect<$stateType> ${lowerFirst(update.name)}(
            |    $stateType state,
            |    ${update.inputType} ${lowerFirst(update.inputType.name)});
            |"""
      }
    }

    val modifiers = if (static) "public static abstract" else "public abstract"

    c"""|$modifiers class $className extends $View<$stateType> {
        |
        |  $handlers
        |}"""
  }

  private[codegen] def abstractViewMultiTable(view: ModelBuilder.ViewService): CodeBlock = {
    import Types.View._

    val viewTableVariables = view.tables.flatMap { table =>
      val transformedUpdates = view.tableTransformedUpdates(table)
      if (transformedUpdates.isEmpty) None
      else {
        val className = view.tableClassName(table)
        Some(c"""private Abstract$className ${lowerFirst(className)};""")
      }
    }

    val viewTableInits = view.tables.flatMap { table =>
      val transformedUpdates = view.tableTransformedUpdates(table)
      if (transformedUpdates.isEmpty) None
      else {
        val className = view.tableClassName(table)
        Some(c"""${lowerFirst(className)} = create$className(context);""")
      }
    }

    val viewTables = view.tables.flatMap { table =>
      val transformedUpdates = view.tableTransformedUpdates(table)
      if (transformedUpdates.isEmpty) None
      else {
        val className = view.tableClassName(table)
        val stateType = view.tableType(table)
        Some(c"""|public Abstract$className ${lowerFirst(className)}() {
                 |  return ${lowerFirst(className)};
                 |}
                 |
                 |public abstract Abstract$className create$className($ViewContext context);
                 |
                 |${abstractViewClass(s"Abstract$className", stateType, transformedUpdates, static = true)}
                 |""")
      }
    }
    c"""|public abstract class ${view.abstractViewName} {
        |
        |  $viewTableVariables
        |
        |  public ${view.abstractViewName}($ViewContext context) {
        |    $viewTableInits
        |  }
        |
        |  $viewTables
        |}"""
  }

}
