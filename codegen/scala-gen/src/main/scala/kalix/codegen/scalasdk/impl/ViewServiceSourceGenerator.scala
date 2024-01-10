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

package kalix.codegen.scalasdk.impl

import kalix.codegen.File
import kalix.codegen.Format
import kalix.codegen.Imports
import kalix.codegen.MessageType
import kalix.codegen.ModelBuilder

/**
 * Responsible for generating Scala sources for a view
 */
object ViewServiceSourceGenerator {
  import kalix.codegen.SourceGeneratorUtils._
  import ScalaGeneratorUtils._

  /**
   * Generate Scala sources the user view source file.
   */
  def generateUnmanaged(service: ModelBuilder.ViewService): Seq[File] =
    Seq(viewSource(service))

  /**
   * Generate Scala sources for provider, handler, abstract baseclass for a view.
   */
  def generateManaged(service: ModelBuilder.ViewService): Seq[File] =
    Seq(abstractView(service), viewRouter(service), viewProvider(service))

  private[codegen] def viewRouter(view: ModelBuilder.ViewService): File = {
    implicit val imports =
      generateImports(
        Seq(view.state.messageType) ++ view.commandTypes,
        view.messageType.parent.scalaPackage,
        otherImports = Seq(
          "kalix.javasdk.impl.view.UpdateHandlerNotFound",
          "kalix.scalasdk.impl.view.ViewRouter",
          "kalix.scalasdk.view.View") ++
          (if (view.tables.size > 1) Seq("kalix.scalasdk.impl.view.ViewMultiTableRouter") else Seq.empty),
        packageImports = Nil)

    File.scala(
      view.messageType.parent.scalaPackage,
      view.routerName,
      s"""|package ${view.messageType.parent.scalaPackage}
          |
          |${writeImports(imports)}
          |
          |$managedComment
          |
          |${viewRouterContent(view)}
          |""".stripMargin)
  }

  private[codegen] def viewRouterContent(view: ModelBuilder.ViewService)(implicit imports: Imports): String = {
    if (view.tables.size > 1) viewRouterMultiTable(view)
    else viewRouterClass(view.routerName, view.className, view.state.messageType, view.transformedUpdates)
  }

  private[codegen] def viewRouterClass(
      className: String,
      viewClassName: String,
      stateType: MessageType,
      transformedUpdates: Iterable[ModelBuilder.Command])(implicit imports: Imports): String = {

    val cases = transformedUpdates.map { cmd =>
      val methodName = cmd.name
      if (cmd.handleDeletes) {
        s"""|case "$methodName" =>
            |  view.${lowerFirst(methodName)}(
            |      state)
            |""".stripMargin
      } else {
        s"""|case "$methodName" =>
            |  view.${lowerFirst(methodName)}(
            |      state,
            |      event.asInstanceOf[${typeName(cmd.inputType)}])
            |""".stripMargin
      }
    }

    s"""|class $className(view: $viewClassName)
        |  extends ViewRouter[${typeName(stateType)}, ${viewClassName}](view) {
        |
        |  override def handleUpdate(
        |      eventName: String,
        |      state: ${typeName(stateType)},
        |      event: Any): View.UpdateEffect[${typeName(stateType)}] = {
        |
        |    eventName match {
        |      ${Format.indent(cases, 6)}
        |
        |      case _ =>
        |        throw new UpdateHandlerNotFound(eventName)
        |    }
        |  }
        |
        |}
        |""".stripMargin
  }

  private[codegen] def viewRouterMultiTable(view: ModelBuilder.ViewService)(implicit imports: Imports): String = {
    val routerClasses = view.tables.flatMap { table =>
      val transformedUpdates = view.tableTransformedUpdates(table)
      if (transformedUpdates.isEmpty) None
      else {
        val className = view.tableClassName(table)
        val stateType = view.tableType(table)
        Some(
          viewRouterClass(
            s"${className}Router",
            s"${view.abstractViewName}#Abstract$className",
            stateType,
            transformedUpdates))
      }
    }
    val routerInstances = view.tables.flatMap { table =>
      if (view.tableTransformedUpdates(table).isEmpty) None
      else {
        val className = view.tableClassName(table)
        Some(s"""|private val ${lowerFirst(className)}Router =
                 |  new ${view.routerName}.${className}Router(view.$className)
                 |""".stripMargin)
      }
    }
    val routerCases = view.transformedUpdates.map { update =>
      val className = view.tableClassName(update.viewTable)
      s"""|case "${update.name}" =>
          |  ${lowerFirst(className)}Router
          |""".stripMargin
    }
    s"""|object ${view.routerName} {
        |  ${Format.indent(routerClasses, 2)}
        |}
        |
        |class ${view.routerName}(view: ${view.className}) extends ViewMultiTableRouter {
        |
        |  ${Format.indent(routerInstances, 2)}
        |
        |  override def viewRouter(eventName: String): ViewRouter[_, _] = {
        |    eventName match {
        |      ${Format.indent(routerCases, 6)}
        |
        |      case _ =>
        |        throw new UpdateHandlerNotFound(eventName)
        |    }
        |  }
        |
        |}
        |""".stripMargin
  }

  private[codegen] def viewProvider(view: ModelBuilder.ViewService): File = {
    implicit val imports =
      generateImports(
        Seq(view.state.messageType, view.messageType.descriptorImport),
        view.messageType.parent.scalaPackage,
        otherImports = Seq(
          "kalix.javasdk.impl.view.UpdateHandlerNotFound",
          "kalix.scalasdk.impl.view.ViewRouter",
          "kalix.scalasdk.view.ViewOptions",
          "kalix.scalasdk.view.ViewProvider",
          "kalix.scalasdk.view.ViewCreationContext",
          "kalix.scalasdk.view.View",
          view.classNameQualified,
          "com.google.protobuf.Descriptors",
          "com.google.protobuf.EmptyProto",
          "scala.collection.immutable.Seq"),
        packageImports = Nil)

    File.scala(
      view.messageType.parent.scalaPackage,
      view.providerName,
      s"""|package ${view.messageType.parent.scalaPackage}
          |
          |${writeImports(imports)}
          |
          |$managedComment
          |
          |object ${view.providerName} {
          |  def apply(viewFactory: ViewCreationContext => ${view.className}): ${view.providerName} =
          |    new ${view.providerName}(viewFactory, viewId = "${view.viewId}", options = ViewOptions.defaults)
          |}
          |
          |class ${view.providerName} private(
          |    viewFactory: ViewCreationContext => ${view.className},
          |    override val viewId: String,
          |    override val options: ViewOptions)
          |  extends ViewProvider {
          |
          |  /**
          |   * Use a custom view identifier. By default, the viewId is the same as the proto service name.
          |   * A different identifier can be needed when making rolling updates with changes to the view definition.
          |   */
          |  def withViewId(viewId: String): ${view.providerName} =
          |    new ${view.providerName}(viewFactory, viewId, options)
          |
          |  def withOptions(newOptions: ViewOptions): ${view.providerName} =
          |    new ${view.providerName}(viewFactory, viewId, newOptions)
          |
          |  override final def serviceDescriptor: Descriptors.ServiceDescriptor =
          |    ${typeName(view.messageType.descriptorImport)}.javaDescriptor.findServiceByName("${view.messageType.protoName}")
          |
          |  override final def newRouter(context: ViewCreationContext): ${view.routerName} =
          |    new ${view.routerName}(viewFactory(context))
          |
          |  override final def additionalDescriptors: Seq[Descriptors.FileDescriptor] =
          |    ${typeName(view.messageType.descriptorImport)}.javaDescriptor ::
          |    Nil
          |}
          |""".stripMargin)
  }

  private[codegen] def viewSource(view: ModelBuilder.ViewService): File = {
    implicit val imports =
      generateImports(
        view.stateTypes ++ view.commandTypes,
        view.messageType.parent.scalaPackage,
        otherImports = Seq("kalix.scalasdk.view.View.UpdateEffect", "kalix.scalasdk.view.ViewContext"),
        packageImports = Nil)

    File.scala(
      view.messageType.parent.scalaPackage,
      view.className,
      s"""|package ${view.messageType.parent.scalaPackage}
          |
          |${writeImports(imports)}
          |
          |$unmanagedComment
          |
          |class ${view.className}(context: ViewContext) extends ${view.abstractViewName}${viewSourceContent(view)}
          |""".stripMargin)
  }

  private[codegen] def viewSourceContent(view: ModelBuilder.ViewService)(implicit imports: Imports): String = {
    if (view.transformedUpdates.isEmpty) ""
    else {
      val content =
        if (view.tables.size > 1) viewSourceMultiTable(view)
        else viewSourceMethods(view.state.messageType, view.transformedUpdates)
      s"""| {
          |$content
          |}""".stripMargin
    }
  }

  private[codegen] def viewSourceMethods(stateType: MessageType, transformedUpdates: Iterable[ModelBuilder.Command])(
      implicit imports: Imports): String = {

    val emptyStateMethod =
      s"""|override def emptyState: ${typeName(stateType)} =
          |  throw new UnsupportedOperationException("Not implemented yet, replace with your empty view state")
          |""".stripMargin

    val handlerMethods = transformedUpdates.toSeq.map { update =>
      val stateType = typeName(update.outputType)
      if (update.handleDeletes) {
        s"""|override def ${lowerFirst(update.name)}(
            |    state: $stateType): UpdateEffect[$stateType] =
            |  throw new UnsupportedOperationException("Delete handler for '${update.name}' not implemented yet")
            |""".stripMargin
      } else {
        s"""|override def ${lowerFirst(update.name)}(
            |    state: $stateType,
            |    ${lowerFirst(update.inputType.name)}: ${typeName(update.inputType)}): UpdateEffect[$stateType] =
            |  throw new UnsupportedOperationException("Update handler for '${update.name}' not implemented yet")
            |""".stripMargin
      }
    }

    val methods = emptyStateMethod +: handlerMethods

    s"""|
        |  ${Format.indent(methods, 2)}
        |""".stripMargin
  }

  private[codegen] def viewSourceMultiTable(view: ModelBuilder.ViewService)(implicit imports: Imports): String = {
    val viewTables = view.tables.flatMap { table =>
      val transformedUpdates = view.tableTransformedUpdates(table)
      if (transformedUpdates.isEmpty) None
      else {
        val className = view.tableClassName(table)
        val stateType = view.tableType(table)
        val methods = viewSourceMethods(stateType, transformedUpdates)
        Some(s"""|object $className extends Abstract$className {
                 |$methods
                 |}
                 |""".stripMargin)
      }
    }
    s"""|
        |  ${Format.indent(viewTables, 2)}
        |""".stripMargin
  }

  private[codegen] def abstractView(view: ModelBuilder.ViewService): File = {
    implicit val imports =
      generateImports(
        Seq(view.state.messageType) ++ view.commandTypes,
        view.messageType.parent.scalaPackage,
        otherImports = Seq("kalix.scalasdk.view.View"),
        packageImports = Nil)

    File.scala(
      view.messageType.parent.scalaPackage,
      view.abstractViewName,
      s"""|package ${view.messageType.parent.scalaPackage}
          |
          |${writeImports(imports)}
          |
          |$managedComment
          |
          |${abstractViewContent(view)}
          |""".stripMargin)
  }

  private[codegen] def abstractViewContent(view: ModelBuilder.ViewService)(implicit imports: Imports): String = {
    if (view.tables.size > 1) abstractViewMultiTable(view)
    else abstractViewClass(view.abstractViewName, view.state.messageType, view.transformedUpdates)
  }

  private[codegen] def abstractViewClass(
      className: String,
      stateType: MessageType,
      transformedUpdates: Iterable[ModelBuilder.Command])(implicit imports: Imports): String = {

    val methods =
      if (transformedUpdates.isEmpty)
        Seq(s"""|override def emptyState: ${typeName(stateType)} =
                |  null // emptyState is only used with transform_updates=true
                |""".stripMargin)
      else
        transformedUpdates.map { update =>
          val stateType = typeName(update.outputType)
          if (update.handleDeletes) {
            s"""|def ${lowerFirst(update.name)}(
                |    state: $stateType): View.UpdateEffect[$stateType]
                |""".stripMargin
          } else {
            s"""|def ${lowerFirst(update.name)}(
                |    state: $stateType,
                |    ${lowerFirst(update.inputType.name)}: ${typeName(update.inputType)}): View.UpdateEffect[$stateType]
                |""".stripMargin
          }
        }

    s"""|abstract class $className extends View[${typeName(stateType)}] {
        |  ${Format.indent(methods, 2)}
        |}""".stripMargin
  }

  private[codegen] def abstractViewMultiTable(view: ModelBuilder.ViewService)(implicit imports: Imports): String = {
    val viewTables = view.tables.flatMap { table =>
      val transformedUpdates = view.tableTransformedUpdates(table)
      if (transformedUpdates.isEmpty) None
      else {
        val className = view.tableClassName(table)
        val stateType = view.tableType(table)
        Some(s"""|def $className: Abstract$className
                 |
                 |${abstractViewClass(s"Abstract$className", stateType, transformedUpdates)}
                 |""".stripMargin)
      }
    }
    s"""|abstract class ${view.abstractViewName} {
        |
        |  ${Format.indent(viewTables, 2)}
        |
        |}""".stripMargin
  }

}
