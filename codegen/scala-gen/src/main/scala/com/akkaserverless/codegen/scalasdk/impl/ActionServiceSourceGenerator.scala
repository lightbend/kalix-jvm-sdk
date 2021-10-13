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
import com.lightbend.akkasls.codegen.Imports
import com.lightbend.akkasls.codegen.Format
import com.lightbend.akkasls.codegen.ModelBuilder

/**
 * Responsible for generating Scala sourced for Actions
 */
object ActionServiceSourceGenerator {

  import com.lightbend.akkasls.codegen.SourceGeneratorUtils._
  import ScalaGeneratorUtils._

  /**
   * Generate Scala sources the user view source file.
   */
  def generateUnmanaged(service: ModelBuilder.ActionService): Seq[File] = Seq(actionSource(service))

  /**
   * Generate Scala sources for provider, handler, abstract baseclass for a view.
   */
  def generateManaged(service: ModelBuilder.ActionService): Seq[File] =
    Seq(abstractAction(service), actionHandler(service), actionProvider(service))

  private def streamImports(commands: Iterable[ModelBuilder.Command]): Seq[String] = {
    if (commands.exists(_.hasStream))
      "akka.NotUsed" :: "akka.stream.scaladsl.Source" :: Nil
    else
      Nil
  }

  private[codegen] def actionSource(service: ModelBuilder.ActionService): File = {

    val className = service.className

    implicit val imports = generateImports(
      service.commandTypes,
      service.fqn.parent.scalaPackage,
      otherImports = Seq(
        "com.akkaserverless.scalasdk.action.Action",
        "com.akkaserverless.scalasdk.action.ActionCreationContext") ++ streamImports(service.commands))

    val methods = service.commands.map { cmd =>
      val methodName = cmd.name
      val input = lowerFirst(cmd.inputType.name)
      val inputType = typeName(cmd.inputType)
      val outputType = typeName(cmd.outputType)

      if (cmd.isUnary) {
        val jsonTopicHint = {
          // note: the somewhat funky indenting is on purpose to lf+indent only if comment present
          if (cmd.inFromTopic && cmd.inputType.fullQualifiedName == "com.google.protobuf.Any")
            """|// JSON input from a topic can be decoded using JsonSupport.decodeJson(classOf[MyClass], any)
               |  """.stripMargin
          else if (cmd.outToTopic && cmd.outputType.fullQualifiedName == "com.google.protobuf.Any")
            """|// JSON output to emit to a topic can be encoded using JsonSupport.encodeJson(myPojo)
               |  """.stripMargin
          else ""
        }

        s"""|/** Handler for "$methodName". */
            |override def ${lowerFirst(methodName)}($input: $inputType): Action.Effect[$outputType] = {
            |  ${jsonTopicHint}throw new RuntimeException("The command handler for `$methodName` is not implemented, yet")
            |}""".stripMargin
      } else if (cmd.isStreamOut) {
        s"""
           |/** Handler for "$methodName". */
           |override def ${lowerFirst(methodName)}($input: $inputType): Source[Action.Effect[$outputType], NotUsed] = {
           |  throw new RuntimeException("The command handler for `$methodName` is not implemented, yet")
           |}""".stripMargin
      } else if (cmd.isStreamIn) {
        s"""
           |/** Handler for "$methodName". */
           |override def ${lowerFirst(methodName)}(${input}Src: Source[$inputType, NotUsed]): Action.Effect[$outputType] = {
           |  throw new RuntimeException("The command handler for `$methodName` is not implemented, yet")
           |}""".stripMargin
      } else {
        s"""
           |/** Handler for "$methodName". */
           |override def ${lowerFirst(methodName)}(${input}Src: Source[$inputType, NotUsed]): Source[Action.Effect[$outputType], NotUsed] = {
           |  throw new RuntimeException("The command handler for `$methodName` is not implemented, yet")
           |}""".stripMargin
      }
    }

    File(
      service.fqn.parent.scalaPackage,
      className,
      s"""|package ${service.fqn.parent.scalaPackage}
        |
        |${writeImports(imports)}
        |
        |$unmanagedComment
        |
        |/** An action. */
        |class $className(creationContext: ActionCreationContext) extends ${service.abstractActionName} {
        |
        |  ${Format.indent(methods, 2)}
        |}
        |""".stripMargin)
  }

  private[codegen] def abstractAction(service: ModelBuilder.ActionService): File = {

    implicit val imports = generateImports(
      service.commandTypes,
      service.fqn.parent.scalaPackage,
      otherImports = Seq("com.akkaserverless.scalasdk.action.Action") ++ streamImports(service.commands))

    val methods = service.commands.map { cmd =>
      val methodName = cmd.name
      val input = lowerFirst(cmd.inputType.name)
      val inputType = typeName(cmd.inputType)
      val outputType = typeName(cmd.outputType)

      if (cmd.isUnary) {
        s"""|/** Handler for "$methodName". */
            |def ${lowerFirst(methodName)}($input: $inputType): Action.Effect[$outputType]""".stripMargin
      } else if (cmd.isStreamOut) {
        s"""
           |/** Handler for "$methodName". */
           |def ${lowerFirst(
          methodName)}($input: $inputType): Source[Action.Effect[$outputType], NotUsed]""".stripMargin
      } else if (cmd.isStreamIn) {
        s"""
           |/** Handler for "$methodName". */
           |def ${lowerFirst(
          methodName)}(${input}Src: Source[$inputType, NotUsed]): Action.Effect[$outputType]""".stripMargin
      } else {
        s"""
           |/** Handler for "$methodName". */
           |def ${lowerFirst(
          methodName)}(${input}Src: Source[$inputType, NotUsed]): Source[Action.Effect[$outputType], NotUsed]""".stripMargin
      }
    }

    File(
      service.fqn.parent.scalaPackage,
      service.abstractActionName,
      s"""|package ${service.fqn.parent.scalaPackage}
        |
        |${writeImports(imports)}
        |
        |$managedComment
        |
        |/** An action. */
        |abstract class ${service.abstractActionName} extends Action {
        |
        |  ${Format.indent(methods, 2)}
        |}
        |""".stripMargin)
  }

  private[codegen] def actionHandler(service: ModelBuilder.ActionService): File = {
    implicit val imports = generateImports(
      commandTypes(service.commands),
      service.fqn.parent.scalaPackage,
      otherImports = Seq(
        "com.akkaserverless.javasdk.impl.action.ActionHandler.HandlerNotFound",
        "com.akkaserverless.scalasdk.impl.action.ActionHandler",
        "com.akkaserverless.scalasdk.action.Action",
        "com.akkaserverless.scalasdk.action.MessageEnvelope",
        "akka.NotUsed",
        "akka.stream.scaladsl.Source"))

    val unaryCases = service.commands.filter(_.isUnary).map { cmd =>
      val methodName = cmd.name
      val inputType = typeName(cmd.inputType)

      s"""|case "$methodName" =>
          |  action.${lowerFirst(methodName)}(message.payload.asInstanceOf[$inputType])
          |""".stripMargin
    }

    val streamOutCases = service.commands.filter(_.isStreamOut).map { cmd =>
      val methodName = cmd.name
      val inputType = typeName(cmd.inputType)

      s"""|case "$methodName" =>
          |  action.${lowerFirst(methodName)}(message.payload.asInstanceOf[$inputType])
          |""".stripMargin
    }

    val streamInCases = service.commands.filter(_.isStreamIn).map { cmd =>
      val methodName = cmd.name
      val inputType = typeName(cmd.inputType)

      s"""|case "$methodName" =>
          |  action.${lowerFirst(methodName)}(stream.map(el => el.payload.asInstanceOf[$inputType]))
          |""".stripMargin
    }

    val streamInOutCases = service.commands.filter(_.isStreamInOut).map { cmd =>
      val methodName = cmd.name
      val inputType = typeName(cmd.inputType)

      s"""|case "$methodName" =>
          |  action.${lowerFirst(methodName)}(stream.map(el => el.payload.asInstanceOf[$inputType]))
          |""".stripMargin
    }

    File(
      service.fqn.parent.scalaPackage,
      service.handlerName,
      s"""|package ${service.fqn.parent.scalaPackage}
        |
        |${writeImports(imports)}
        |
        |$managedComment
        |
        |/** A Action handler */
        |class ${service.handlerName}(action: ${service.className}) extends ActionHandler[${service.className}](action) {
        |
        |  override def handleUnary(commandName: String, message: MessageEnvelope[Any]):  Action.Effect[_] = {
        |    commandName match {
        |      ${Format.indent(unaryCases, 6)}
        |      case _ =>
        |        throw new HandlerNotFound(commandName)
        |    }
        |  }
        |
        |  override def handleStreamedOut(commandName: String, message: MessageEnvelope[Any]): Source[Action.Effect[_], NotUsed] = {
        |    commandName match {
        |      ${Format.indent(streamOutCases, 6)}
        |      case _ =>
        |        throw new HandlerNotFound(commandName)
        |    }
        |  }
        |
        |  override def handleStreamedIn(commandName: String, stream: Source[MessageEnvelope[Any], NotUsed]): Action.Effect[_] = {
        |    commandName match {
        |      ${Format.indent(streamInCases, 6)}
        |      case _ =>
        |        throw new HandlerNotFound(commandName)
        |    }
        |  }
        |
        |  override def handleStreamed(commandName: String, stream: Source[MessageEnvelope[Any], NotUsed]): Source[Action.Effect[_], NotUsed] = {
        |    commandName match {
        |      ${Format.indent(streamInOutCases, 6)}
        |      case _ =>
        |        throw new HandlerNotFound(commandName)
        |    }
        |  }
        |}
        |""".stripMargin)
  }

  private[codegen] def actionProvider(service: ModelBuilder.ActionService): File = {
    implicit val imports: Imports = generateImports(
      commandTypes(service.commands),
      service.fqn.parent.scalaPackage,
      otherImports = Seq(
        "com.akkaserverless.scalasdk.action.ActionProvider",
        "com.akkaserverless.scalasdk.action.ActionCreationContext",
        "com.akkaserverless.scalasdk.action.ActionOptions",
        "com.google.protobuf.Descriptors",
        "scala.collection.immutable"))

    File(
      service.fqn.parent.scalaPackage,
      service.providerName,
      s"""|package ${service.fqn.parent.scalaPackage}
        |
        |${writeImports(imports)}
        |
        |$managedComment
        |
        |object ${service.providerName} {
        |  def apply(actionFactory: ActionCreationContext => ${service.className}): ${service.providerName} =
        |    new ${service.providerName}(actionFactory, ActionOptions.defaults)
        |
        |  def apply(actionFactory: ActionCreationContext => ${service.className}, options: ActionOptions): ${service.providerName} =
        |    new ${service.providerName}(actionFactory, options)
        |}
        |
        |class ${service.providerName} private(actionFactory: ActionCreationContext => ${service.className},
        |                                      override val options: ActionOptions)
        |  extends ActionProvider[${service.className}] {
        |
        |  override final def serviceDescriptor: Descriptors.ServiceDescriptor =
        |    ${typeName(service.fqn.descriptorImport)}.javaDescriptor.findServiceByName("${service.fqn.protoName}")
        |
        |  override final def newHandler(context: ActionCreationContext): ${service.handlerName} =
        |    new ${service.handlerName}(actionFactory(context))
        |
        |  override final def additionalDescriptors: immutable.Seq[Descriptors.FileDescriptor] =
        |    ${typeName(service.fqn.descriptorImport)}.javaDescriptor ::
        |    Nil
        |
        |  def withOptions(options: ActionOptions): ${service.providerName} =
        |    new ${service.providerName}(actionFactory, options)
        |}
        |""".stripMargin)
  }
}
