/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.codegen.scalasdk.impl

import kalix.codegen.File
import kalix.codegen.ModelBuilder
import kalix.codegen.PackageNaming
import kalix.codegen.ClassMessageType

/**
 * Responsible for generating Scala sourced for Actions
 */
object ActionServiceSourceGenerator {

  import ScalaGeneratorUtils._
  import kalix.codegen.SourceGeneratorUtils._

  /**
   * Generate Scala sources the user view source file.
   */
  def generateUnmanaged(service: ModelBuilder.ActionService): Seq[File] =
    Seq(actionSource(service))

  /**
   * Generate Scala sources for provider, handler, abstract baseclass for a view.
   */
  def generateManaged(service: ModelBuilder.ActionService, mainPackageName: PackageNaming): Seq[File] =
    Seq(abstractAction(service, mainPackageName), actionRouter(service), actionProvider(service))

  private[codegen] def actionSource(service: ModelBuilder.ActionService): File = {
    import Types.{ Source, NotUsed }
    import Types.Action._

    val className = service.className
    val commands = service.commands.filterNot(_.ignore)

    val methods = commands.map { cmd =>
      val methodName = cmd.name
      val input = lowerFirst(cmd.inputType.name)
      val inputType = cmd.inputType
      val outputType = cmd.outputType

      if (cmd.isUnary) {
        val jsonTopicHint: CodeElement = {
          // note: the somewhat funky indenting is on purpose to lf+indent only if comment present
          if (cmd.inFromTopic && cmd.inputType.fullyQualifiedProtoName == "com.google.protobuf.Any")
            c"""|// JSON input from a topic can be decoded using JsonSupport.decodeJson(classOf[MyClass], any)
                |  """
          else if (cmd.outToTopic && cmd.outputType.fullyQualifiedProtoName == "com.google.protobuf.Any")
            c"""|// JSON output to emit to a topic can be encoded using JsonSupport.encodeJson(myPojo)
                |  """
          else ""
        }

        if (cmd.handleDeletes) {
          c"""|override def ${lowerFirst(methodName)}(): $Action.Effect[$outputType] = {
              |  ${jsonTopicHint}throw new RuntimeException("The delete handler for `$methodName` is not implemented, yet")
              |}"""
        } else {
          c"""|override def ${lowerFirst(methodName)}($input: $inputType): $Action.Effect[$outputType] = {
              |  ${jsonTopicHint}throw new RuntimeException("The command handler for `$methodName` is not implemented, yet")
              |}"""
        }
      } else if (cmd.isStreamOut) {
        c"""
           |override def ${lowerFirst(methodName)}($input: $inputType): $Source[$Action.Effect[$outputType], $NotUsed] = {
           |  throw new RuntimeException("The command handler for `$methodName` is not implemented, yet")
           |}"""
      } else if (cmd.isStreamIn) {
        c"""
           |override def ${lowerFirst(methodName)}(${input}Src: Source[$inputType, $NotUsed]): $Action.Effect[$outputType] = {
           |  throw new RuntimeException("The command handler for `$methodName` is not implemented, yet")
           |}"""
      } else {
        c"""
           |override def ${lowerFirst(methodName)}(${input}Src: Source[$inputType, $NotUsed]): $Source[$Action.Effect[$outputType], $NotUsed] = {
           |  throw new RuntimeException("The command handler for `$methodName` is not implemented, yet")
           |}"""
      }
    }

    generate(
      service.messageType.parent,
      className,
      c"""|$unmanagedComment
          |
          |class $className(creationContext: $ActionCreationContext) extends ${service.abstractActionName} {
          |
          |  $methods
          |}
          |""")
  }

  private[codegen] def abstractAction(service: ModelBuilder.ActionService, mainPackageName: PackageNaming): File = {
    import Types.{ Source, NotUsed }
    import Types.Action._

    val commands = service.commands.filterNot(_.ignore)
    val methods = commands.map { cmd =>
      val methodName = cmd.name
      val input = lowerFirst(cmd.inputType.name)
      val inputType = cmd.inputType
      val outputType = cmd.outputType

      if (cmd.isUnary) {
        if (cmd.handleDeletes) {
          c"""|def ${lowerFirst(methodName)}(): $Action.Effect[$outputType]"""
        } else {
          c"""|def ${lowerFirst(methodName)}($input: $inputType): $Action.Effect[$outputType]"""
        }
      } else if (cmd.isStreamOut) {
        c"""
           |def ${lowerFirst(methodName)}($input: $inputType): $Source[$Action.Effect[$outputType], $NotUsed]"""
      } else if (cmd.isStreamIn) {
        c"""
           |def ${lowerFirst(methodName)}(${input}Src: $Source[$inputType, $NotUsed]): $Action.Effect[$outputType]"""
      } else {
        c"""
           |def ${lowerFirst(
          methodName)}(${input}Src: $Source[$inputType, $NotUsed]): $Source[$Action.Effect[$outputType], $NotUsed]"""
      }
    }

    val Components = ClassMessageType(mainPackageName.javaPackage + ".Components")
    val ComponentsImpl = ClassMessageType(mainPackageName.javaPackage + ".ComponentsImpl")

    generate(
      service.messageType.parent,
      service.abstractActionName,
      c"""|$managedComment
          |
          |abstract class ${service.abstractActionName} extends $Action {
          |
          |  def components: $Components =
          |    new ${ComponentsImpl}(contextForComponents)
          |
          |  $methods
          |}
          |""")
  }

  private[codegen] def actionRouter(service: ModelBuilder.ActionService): File = {
    import Types.{ Source, NotUsed }
    import Types.Action._

    val commands = service.commands.filterNot(_.ignore)

    val unaryCases = commands.filter(_.isUnary).map { cmd =>
      val methodName = cmd.name
      val inputType = cmd.inputType

      if (cmd.handleDeletes) {
        c"""|case "$methodName" =>
            |  action.${lowerFirst(methodName)}()
            |"""
      } else {
        c"""|case "$methodName" =>
            |  action.${lowerFirst(methodName)}(message.payload.asInstanceOf[$inputType])
            |"""
      }
    }

    val streamOutCases = commands.filter(_.isStreamOut).map { cmd =>
      val methodName = cmd.name
      val inputType = cmd.inputType

      c"""|case "$methodName" =>
          |  action.${lowerFirst(methodName)}(message.payload.asInstanceOf[$inputType])
          |"""
    }

    val streamInCases = commands.filter(_.isStreamIn).map { cmd =>
      val methodName = cmd.name
      val inputType = cmd.inputType

      c"""|case "$methodName" =>
          |  action.${lowerFirst(methodName)}(stream.map(el => el.payload.asInstanceOf[$inputType]))
          |"""
    }

    val streamInOutCases = commands.filter(_.isStreamInOut).map { cmd =>
      val methodName = cmd.name
      val inputType = cmd.inputType

      c"""|case "$methodName" =>
          |  action.${lowerFirst(methodName)}(stream.map(el => el.payload.asInstanceOf[$inputType]))
          |"""
    }

    generate(
      service.messageType.parent,
      service.routerName,
      c"""|$managedComment
          |
          |class ${service.routerName}(action: ${service.className}) extends $ActionRouter[${service.className}](action) {
          |
          |  override def handleUnary(commandName: String, message: $MessageEnvelope[Any]):  $Action.Effect[_] = {
          |    commandName match {
          |      $unaryCases
          |      case _ =>
          |        throw new $HandlerNotFound(commandName)
          |    }
          |  }
          |
          |  override def handleStreamedOut(commandName: String, message: $MessageEnvelope[Any]): $Source[$Action.Effect[_], $NotUsed] = {
          |    commandName match {
          |      $streamOutCases
          |      case _ =>
          |        throw new $HandlerNotFound(commandName)
          |    }
          |  }
          |
          |  override def handleStreamedIn(commandName: String, stream: $Source[$MessageEnvelope[Any], $NotUsed]): $Action.Effect[_] = {
          |    commandName match {
          |      $streamInCases
          |      case _ =>
          |        throw new $HandlerNotFound(commandName)
          |    }
          |  }
          |
          |  override def handleStreamed(commandName: String, stream: $Source[$MessageEnvelope[Any], $NotUsed]): $Source[$Action.Effect[_], $NotUsed] = {
          |    commandName match {
          |      $streamInOutCases
          |      case _ =>
          |        throw new $HandlerNotFound(commandName)
          |    }
          |  }
          |}
          |""")
  }

  private[codegen] def actionProvider(service: ModelBuilder.ActionService): File = {
    import Types.{ Descriptors, ImmutableSeq }
    import Types.Action._

    generate(
      service.messageType.parent,
      service.providerName,
      c"""|$managedComment
          |
          |object ${service.providerName} {
          |  def apply(actionFactory: $ActionCreationContext => ${service.className}): ${service.providerName} =
          |    new ${service.providerName}(actionFactory, $ActionOptions.defaults)
          |
          |  def apply(actionFactory: $ActionCreationContext => ${service.className}, options: $ActionOptions): ${service.providerName} =
          |    new ${service.providerName}(actionFactory, options)
          |}
          |
          |class ${service.providerName} private(actionFactory: $ActionCreationContext => ${service.className},
          |                                      override val options: $ActionOptions)
          |  extends $ActionProvider[${service.className}] {
          |
          |  override final def serviceDescriptor: $Descriptors.ServiceDescriptor =
          |    ${service.messageType.descriptorImport}.javaDescriptor.findServiceByName("${service.messageType.protoName}")
          |
          |  override final def newRouter(context: $ActionCreationContext): ${service.routerName} =
          |    new ${service.routerName}(actionFactory(context))
          |
          |  override final def additionalDescriptors: $ImmutableSeq[$Descriptors.FileDescriptor] =
          |    ${service.messageType.descriptorImport}.javaDescriptor ::
          |    Nil
          |
          |  def withOptions(options: $ActionOptions): ${service.providerName} =
          |    new ${service.providerName}(actionFactory, options)
          |}
          |""")
  }
}
