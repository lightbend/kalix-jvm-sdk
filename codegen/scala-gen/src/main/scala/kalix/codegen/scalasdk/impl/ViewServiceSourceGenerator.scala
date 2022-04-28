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

package kalix.codegen.scalasdk.impl

import kalix.codegen.File
import kalix.codegen.Format
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
          "kalix.scalasdk.view.View"),
        packageImports = Nil)

    val cases = view.transformedUpdates
      .map { cmd =>
        val methodName = cmd.name
        s"""|case "$methodName" =>
            |  view.${lowerFirst(methodName)}(
            |      state,
            |      event.asInstanceOf[${typeName(cmd.inputType)}])
            |""".stripMargin
      }

    File.scala(
      view.messageType.parent.scalaPackage,
      view.routerName,
      s"""|package ${view.messageType.parent.scalaPackage}
        |
        |${writeImports(imports)}
        |
        |$managedComment
        |
        |class ${view.routerName}(view: ${view.className})
        |  extends ViewRouter[${typeName(view.state.messageType)}, ${view.className}](view) {
        |
        |  override def handleUpdate(
        |      eventName: String,
        |      state: ${typeName(view.state.messageType)},
        |      event: Any): View.UpdateEffect[${typeName(view.state.messageType)}] = {
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
        |""".stripMargin)
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
        |  extends ViewProvider[${typeName(view.state.messageType)}, ${view.className}] {
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
        Seq(view.state.messageType) ++ view.commandTypes,
        view.messageType.parent.scalaPackage,
        otherImports = Seq("kalix.scalasdk.view.View.UpdateEffect", "kalix.scalasdk.view.ViewContext"),
        packageImports = Nil)

    val emptyState =
      if (view.transformedUpdates.isEmpty)
        ""
      else
        s"""|  override def emptyState: ${typeName(view.state.messageType)} =
            |    throw new UnsupportedOperationException("Not implemented yet, replace with your empty view state")
            |""".stripMargin

    val handlers = view.transformedUpdates.map { update =>
      val stateType = typeName(update.outputType)
      s"""override def ${lowerFirst(update.name)}(
         |  state: $stateType, ${lowerFirst(update.inputType.name)}: ${typeName(update.inputType)}): UpdateEffect[$stateType] =
         |  throw new UnsupportedOperationException("Update handler for '${update.name}' not implemented yet")
         |""".stripMargin
    }

    File.scala(
      view.messageType.parent.scalaPackage,
      view.className,
      s"""|package ${view.messageType.parent.scalaPackage}
        |
        |${writeImports(imports)}
        |
        |$unmanagedComment
        |
        |class ${view.className}(context: ViewContext) extends ${view.abstractViewName} {
        |
        |$emptyState
        |  ${Format.indent(handlers, 2)}
        |}
        |""".stripMargin)
  }

  private[codegen] def abstractView(view: ModelBuilder.ViewService): File = {
    implicit val imports =
      generateImports(
        Seq(view.state.messageType) ++ view.commandTypes,
        view.messageType.parent.scalaPackage,
        otherImports = Seq("kalix.scalasdk.view.View"),
        packageImports = Nil)

    val emptyState =
      if (view.transformedUpdates.isEmpty)
        s"""|  override def emptyState: ${typeName(view.state.messageType)} =
            |    null // emptyState is only used with transform_updates=true
            |""".stripMargin
      else
        ""

    val handlers = view.transformedUpdates.map { update =>
      val stateType = typeName(update.outputType)
      s"""def ${lowerFirst(update.name)}(
         |  state: $stateType, ${lowerFirst(update.inputType.name)}: ${typeName(
        update.inputType)}): View.UpdateEffect[$stateType]""".stripMargin

    }

    File.scala(
      view.messageType.parent.scalaPackage,
      view.abstractViewName,
      s"""|package ${view.messageType.parent.scalaPackage}
        |
        |${writeImports(imports)}
        |
        |$managedComment
        |
        |abstract class ${view.abstractViewName} extends View[${typeName(view.state.messageType)}] {
        |
        |$emptyState
        |  ${Format.indent(handlers, 2)}
        |}
        |""".stripMargin)
  }

}
