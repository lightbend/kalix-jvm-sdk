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

package com.akkaserverless.codegen.scalasdk.impl

import com.akkaserverless.codegen.scalasdk.File
import com.lightbend.akkasls.codegen.Format
import com.lightbend.akkasls.codegen.ModelBuilder

/**
 * Responsible for generating Scala sources for a view
 */
object ViewServiceSourceGenerator {
  import com.lightbend.akkasls.codegen.SourceGeneratorUtils._

  /**
   * Generate Scala sources the user view source file.
   */
  def generateUnmanaged(service: ModelBuilder.ViewService): Iterable[File] = {
    val generatedSources = Seq.newBuilder[File]

    val packageName = service.fqn.parent.scalaPackage
    val packagePath = packageAsPath(packageName)

    generatedSources += File(s"$packagePath/${service.className}.scala", viewSource(service, packageName))

    generatedSources.result()
  }

  /**
   * Generate Scala sources for provider, handler, abstract baseclass for a view.
   */
  def generateManaged(service: ModelBuilder.ViewService): Iterable[File] = {
    val generatedSources = Seq.newBuilder[File]

    val packageName = service.fqn.parent.scalaPackage
    val packagePath = packageAsPath(packageName)

    generatedSources += File(s"$packagePath/${service.abstractViewName}.scala", abstractView(service, packageName))
    generatedSources += File(s"$packagePath/${service.handlerName}.scala", viewHandler(service, packageName))
    generatedSources += File(s"$packagePath/${service.providerName}.scala", viewProvider(service, packageName))

    generatedSources.result()
  }

  private[codegen] def viewHandler(view: ModelBuilder.ViewService, packageName: String): String = {
    val imports = generateCommandImports(
      view.commands,
      view.state,
      packageName,
      otherImports = Seq(
        "com.akkaserverless.javasdk.impl.view.UpdateHandlerNotFound",
        "com.akkaserverless.scalasdk.impl.view.ViewHandler",
        "com.akkaserverless.scalasdk.view.View"),
      semi = false)

    val cases = view.transformedUpdates
      .map { cmd =>
        val methodName = cmd.name
        val inputType = qualifiedType(cmd.inputType)
        s"""|case "$methodName" =>
            |  view.${lowerFirst(methodName)}(
            |      state,
            |      event.asInstanceOf[$inputType])
            |""".stripMargin
      }

    s"""|package $packageName
        |
        |$imports
        |
        |/** A view handler */
        |class ${view.handlerName}(view: ${view.className}) extends ViewHandler[${qualifiedType(view.state.fqn)}, ${view.className}](view) {
        |
        |  override def handleUpdate(
        |      eventName: String,
        |      state: ${qualifiedType(view.state.fqn)},
        |      event: Any): View.UpdateEffect[${qualifiedType(view.state.fqn)}] = {
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

  private[codegen] def viewProvider(view: ModelBuilder.ViewService, packageName: String): String = {
    val imports = generateCommandImports(
      Nil,
      view.state,
      packageName,
      otherImports = Seq(
        "com.akkaserverless.javasdk.impl.view.UpdateHandlerNotFound",
        "com.akkaserverless.scalasdk.impl.view.ViewHandler",
        "com.akkaserverless.scalasdk.view.ViewProvider",
        "com.akkaserverless.scalasdk.view.ViewCreationContext",
        "com.akkaserverless.scalasdk.view.View",
        view.classNameQualified,
        "com.google.protobuf.Descriptors",
        "com.google.protobuf.EmptyProto",
        "scala.collection.immutable"),
      semi = false)

    s"""|package $packageName
        |
        |$imports
        |
        |object ${view.providerName} {
        |  def apply(viewFactory: Function[ViewCreationContext, ${view.className}]): ${view.providerName} =
        |    new ${view.providerName}(viewFactory, viewId = "${view.viewId}")
        |}
        |
        |class ${view.providerName} private(viewFactory: Function[ViewCreationContext, ${view.className}],
        |    override val viewId: String)
        |  extends ViewProvider[${qualifiedType(view.state.fqn)}, ${view.className}] {
        |
        |  /**
        |   * Use a custom view identifier. By default, the viewId is the same as the proto service name.
        |   * A different identifier can be needed when making rolling updates with changes to the view definition.
        |   */
        |  def withViewId(viewId: String): ${view.providerName} =
        |    new ${view.providerName}(viewFactory, viewId)
        |
        |  override final def serviceDescriptor: Descriptors.ServiceDescriptor =
        |    ${view.fqn.parent.javaOuterClassname}.getDescriptor().findServiceByName("${view.fqn.protoName}")
        |
        |  override final def newHandler(context: ViewCreationContext): ${view.handlerName} =
        |    new ${view.handlerName}(viewFactory(context))
        |
        |  override final def additionalDescriptors: immutable.Seq[Descriptors.FileDescriptor] =
        |    ${view.fqn.parent.javaOuterClassname}.getDescriptor() ::
        |    Nil
        |}
        |""".stripMargin
  }

  private[codegen] def viewSource(view: ModelBuilder.ViewService, packageName: String): String = {

    val imports =
      generateImports(
        view.commandTypes,
        packageName,
        Seq("com.akkaserverless.scalasdk.view.View.UpdateEffect", "com.akkaserverless.scalasdk.view.ViewContext"),
        semi = false)

    val emptyState =
      if (view.transformedUpdates.isEmpty)
        ""
      else
        s"""|  override def emptyState: ${qualifiedType(view.state.fqn)} =
            |    throw new UnsupportedOperationException("Not implemented yet, replace with your empty view state")
            |""".stripMargin

    val handlers = view.transformedUpdates.map { update =>
      val stateType = qualifiedType(update.outputType)
      s"""override def ${lowerFirst(update.name)}(
         |  state: $stateType, ${lowerFirst(update.inputType.name)}: ${qualifiedType(update.inputType)}): UpdateEffect[$stateType] =
         |  throw new UnsupportedOperationException("Update handler for '${update.name}' not implemented yet")
         |""".stripMargin
    }

    s"""|package $packageName
        |
        |$imports
        |
        |class ${view.className}(context: ViewContext) extends ${view.abstractViewName} {
        |
        |$emptyState
        |  ${Format.indent(handlers, 2)}
        |}
        |""".stripMargin
  }

  private[codegen] def abstractView(view: ModelBuilder.ViewService, packageName: String): String = {
    val imports =
      generateImports(view.commandTypes, packageName, Seq("com.akkaserverless.scalasdk.view.View"), semi = false)

    val emptyState =
      if (view.transformedUpdates.isEmpty)
        s"""|  override def emptyState: ${qualifiedType(view.state.fqn)} =
            |    null // emptyState is only used with transform_updates=true
            |""".stripMargin
      else
        ""

    val handlers = view.transformedUpdates.map { update =>
      val stateType = qualifiedType(update.outputType)
      s"""def ${lowerFirst(update.name)}(
         |  state: $stateType, ${lowerFirst(update.inputType.name)}: ${qualifiedType(
        update.inputType)}): View.UpdateEffect[$stateType]""".stripMargin

    }

    s"""|package $packageName
        |
        |$imports
        |
        |abstract class ${view.abstractViewName} extends View[${qualifiedType(view.state.fqn)}] {
        |
        |$emptyState
        |  ${Format.indent(handlers, 2)}
        |}
        |""".stripMargin
  }

}
