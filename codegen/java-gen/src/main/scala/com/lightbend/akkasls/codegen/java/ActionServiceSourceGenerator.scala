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

import _root_.java.nio.file.Files
import _root_.java.nio.file.Path

import com.google.common.base.Charsets

/**
 * Responsible for generating Java source from an entity model
 */
object ActionServiceSourceGenerator {
  import SourceGenerator._
  import com.lightbend.akkasls.codegen.SourceGeneratorUtils._

  /**
   * Generate Java source from views where the target source and test source directories have no existing source.
   *
   * Also generates a main source file if it does not already exist.
   *
   * Impure.
   */
  def generate(
      service: ModelBuilder.ActionService,
      sourceDirectory: Path,
      generatedSourceDirectory: Path): Iterable[Path] = {

    val packageName = service.fqn.parent.javaPackage
    val packagePath = packageAsPath(packageName)

    val implSourcePath =
      sourceDirectory.resolve(packagePath.resolve(service.className + ".java"))

    val interfaceSourcePath =
      generatedSourceDirectory.resolve(packagePath.resolve(service.interfaceName + ".java"))

    interfaceSourcePath.getParent.toFile.mkdirs()
    Files.write(interfaceSourcePath, abstractActionSource(service).getBytes(Charsets.UTF_8))

    val handlerSourcePath = generatedSourceDirectory.resolve(packagePath.resolve(service.handlerName + ".java"))
    handlerSourcePath.getParent.toFile.mkdirs()
    Files.write(handlerSourcePath, actionHandler(service).getBytes(Charsets.UTF_8))

    val providerSourcePath = generatedSourceDirectory.resolve(packagePath.resolve(service.providerName + ".java"))
    providerSourcePath.getParent.toFile.mkdirs()
    Files.write(providerSourcePath, actionProvider(service).getBytes(Charsets.UTF_8))

    if (!implSourcePath.toFile.exists()) {
      // Now we generate the entity
      implSourcePath.getParent.toFile.mkdirs()
      Files.write(implSourcePath, actionSource(service).getBytes(Charsets.UTF_8))
    }
    // We return implSourcePath here even when we didn't just generate it, because
    // otherwise the incremental compiler can't seem to find it. I'm not entirely confident
    // this is the right way to fix that, but it seems to work. Let's revisit when we find a
    // problem with this approach.
    List(implSourcePath, interfaceSourcePath, providerSourcePath, handlerSourcePath)
  }

  private def isUnary(cmd: ModelBuilder.Command): Boolean = !cmd.streamedInput && !cmd.streamedOutput
  private def isStreamIn(cmd: ModelBuilder.Command): Boolean = cmd.streamedInput && !cmd.streamedOutput
  private def isStreamOut(cmd: ModelBuilder.Command): Boolean = !cmd.streamedInput && cmd.streamedOutput
  private def isStreamInOut(cmd: ModelBuilder.Command): Boolean = cmd.streamedInput && cmd.streamedOutput
  private def hasStream(cmd: ModelBuilder.Command): Boolean = isStreamIn(cmd) || isStreamOut(cmd) || isStreamInOut(cmd)

  private def streamImports(commands: Iterable[ModelBuilder.Command]): Seq[String] = {
    if (commands.exists(c => hasStream(c)))
      "akka.NotUsed" :: "akka.stream.javadsl.Source" :: Nil
    else
      Nil
  }

  private[codegen] def actionSource(service: ModelBuilder.ActionService): String = {

    val packageName = service.fqn.parent.javaPackage
    val className = service.className

    val imports = generateImports(
      service.commandTypes,
      packageName,
      otherImports = Seq("com.akkaserverless.javasdk.action.ActionCreationContext") ++ streamImports(service.commands))

    val methods = service.commands.map { cmd =>
      val methodName = cmd.name
      val input = lowerFirst(cmd.inputType.name)
      val inputTypeFullName = cmd.inputType.fullName
      val outputType = cmd.outputType.fullName

      if (isUnary(cmd)) {
        val jsonTopicHint = {
          // note: the somewhat funky indenting is on purpose to lf+indent only if comment present
          if (cmd.inFromTopic && cmd.inputType.fullQualifiedName == "com.google.protobuf.Any")
            """|// JSON input from a topic can be decoded using JsonSupport.decodeJson(MyClass.class, any)
               |  """.stripMargin
          else if (cmd.outToTopic && cmd.outputType.fullQualifiedName == "com.google.protobuf.Any")
            """|// JSON output to emit to a topic can be encoded using JsonSupport.encodeJson(myPojo)
               |  """.stripMargin
          else ""
        }

        s"""|/** Handler for "$methodName". */
            |@Override
            |public Effect<$outputType> ${lowerFirst(methodName)}($inputTypeFullName $input) {
            |  ${jsonTopicHint}throw new RuntimeException("The command handler for `$methodName` is not implemented, yet");
            |}""".stripMargin
      } else if (isStreamOut(cmd)) {
        s"""
           |/** Handler for "$methodName". */
           |@Override
           |public Source<Effect<$outputType>, NotUsed> ${lowerFirst(methodName)}($inputTypeFullName $input) {
           |  throw new RuntimeException("The command handler for `$methodName` is not implemented, yet");
           |}""".stripMargin
      } else if (isStreamIn(cmd)) {
        s"""
           |/** Handler for "$methodName". */
           |@Override
           |public Effect<$outputType> ${lowerFirst(methodName)}(Source<$inputTypeFullName, NotUsed> ${input}Src) {
           |  throw new RuntimeException("The command handler for `$methodName` is not implemented, yet");
           |}""".stripMargin
      } else {
        s"""
           |/** Handler for "$methodName". */
           |@Override
           |public Source<Effect<$outputType>, NotUsed> ${lowerFirst(methodName)}(Source<$inputTypeFullName, NotUsed> ${input}Src) {
           |  throw new RuntimeException("The command handler for `$methodName` is not implemented, yet");
           |}""".stripMargin
      }
    }

    s"""|$unmanagedComment
        |
        |package $packageName;
        |
        |$imports
        |
        |/** An action. */
        |public class $className extends ${service.interfaceName} {
        |
        |  public $className(ActionCreationContext creationContext) {}
        |
        |  ${Format.indent(methods, 2)}
        |}
        |""".stripMargin
  }

  private[codegen] def abstractActionSource(service: ModelBuilder.ActionService): String = {

    val packageName = service.fqn.parent.javaPackage
    val imports = generateImports(
      service.commandTypes,
      packageName,
      otherImports = Seq("com.akkaserverless.javasdk.action.Action") ++ streamImports(service.commands))

    val methods = service.commands.map { cmd =>
      val methodName = cmd.name
      val input = lowerFirst(cmd.inputType.name)
      val inputTypeFullName = cmd.inputType.fullName
      val outputType = cmd.outputType.fullName

      if (isUnary(cmd)) {
        s"""|/** Handler for "$methodName". */
            |public abstract Effect<$outputType> ${lowerFirst(methodName)}($inputTypeFullName $input);""".stripMargin
      } else if (isStreamOut(cmd)) {
        s"""
           |/** Handler for "$methodName". */
           |public abstract Source<Effect<$outputType>, NotUsed> ${lowerFirst(
          methodName)}($inputTypeFullName $input);""".stripMargin
      } else if (isStreamIn(cmd)) {
        s"""
           |/** Handler for "$methodName". */
           |public abstract Effect<$outputType> ${lowerFirst(
          methodName)}(Source<$inputTypeFullName, NotUsed> ${input}Src);""".stripMargin
      } else {
        s"""
           |/** Handler for "$methodName". */
           |public abstract Source<Effect<$outputType>, NotUsed> ${lowerFirst(
          methodName)}(Source<$inputTypeFullName, NotUsed> ${input}Src);""".stripMargin
      }
    }

    s"""|$managedComment
        |
        |package $packageName;
        |
        |$imports
        |
        |/** An action. */
        |public abstract class ${service.interfaceName} extends Action {
        |
        |  ${Format.indent(methods, 2)}
        |}""".stripMargin
  }

  private[codegen] def actionHandler(service: ModelBuilder.ActionService): String = {

    val className = service.className
    val packageName = service.fqn.parent.javaPackage

    val unaryCases = service.commands.filter(isUnary).map { cmd =>
      val methodName = cmd.name
      val inputTypeFullName = cmd.inputType.fullName

      s"""|case "$methodName":
          |  return action()
          |           .${lowerFirst(methodName)}(($inputTypeFullName) message.payload());
          |""".stripMargin
    }

    val streamOutCases = service.commands.filter(isStreamOut).map { cmd =>
      val methodName = cmd.name
      val inputTypeFullName = cmd.inputType.fullName

      s"""|case "$methodName":
          |  return action()
          |           .${lowerFirst(methodName)}(($inputTypeFullName) message.payload());
          |""".stripMargin
    }

    val streamInCases = service.commands.filter(isStreamIn).map { cmd =>
      val methodName = cmd.name
      val inputTypeFullName = cmd.inputType.fullName

      s"""|case "$methodName":
          |  return action()
          |           .${lowerFirst(methodName)}(stream.map(el -> ($inputTypeFullName) el.payload()));
          |""".stripMargin
    }

    val streamInOutCases = service.commands.filter(isStreamInOut).map { cmd =>
      val methodName = cmd.name
      val inputTypeFullName = cmd.inputType.fullName

      s"""|case "$methodName":
          |  return action()
          |           .${lowerFirst(methodName)}(stream.map(el -> ($inputTypeFullName) el.payload()));
          |""".stripMargin
    }

    val imports = generateImports(
      service.commandTypes,
      packageName,
      otherImports = Seq(
        "akka.NotUsed",
        "akka.stream.javadsl.Source",
        "com.akkaserverless.javasdk.action.Action",
        "com.akkaserverless.javasdk.action.MessageEnvelope",
        "com.akkaserverless.javasdk.impl.action.ActionHandler"))

    s"""|$managedComment
        |
        |package $packageName;
        |
        |$imports
        |
        |public class ${service.handlerName} extends ActionHandler<$className> {
        |
        |  public ${service.handlerName}($className actionBehavior) {
        |    super(actionBehavior);
        |  }
        |
        |  @Override
        |  public Action.Effect<?> handleUnary(String commandName, MessageEnvelope<Object> message) {
        |    switch (commandName) {
        |      ${Format.indent(unaryCases, 6)}
        |      default:
        |        throw new ActionHandler.HandlerNotFound(commandName);
        |    }
        |  }
        |
        |  @Override
        |  public Source<Action.Effect<?>, NotUsed> handleStreamedOut(String commandName, MessageEnvelope<Object> message) {
        |    switch (commandName) {
        |      ${Format.indent(streamOutCases, 6)}
        |      default:
        |        throw new ActionHandler.HandlerNotFound(commandName);
        |    }
        |  }
        |
        |  @Override
        |  public Action.Effect<?> handleStreamedIn(String commandName, Source<MessageEnvelope<Object>, NotUsed> stream) {
        |    switch (commandName) {
        |      ${Format.indent(streamInCases, 6)}
        |      default:
        |        throw new ActionHandler.HandlerNotFound(commandName);
        |    }
        |  }
        |
        |  @Override
        |  public Source<Action.Effect<?>, NotUsed> handleStreamed(String commandName, Source<MessageEnvelope<Object>, NotUsed> stream) {
        |    switch (commandName) {
        |      ${Format.indent(streamInOutCases, 6)}
        |      default:
        |        throw new ActionHandler.HandlerNotFound(commandName);
        |    }
        |  }
        |}
        |""".stripMargin
  }

  private[codegen] def actionProvider(service: ModelBuilder.ActionService): String = {

    val packageName = service.fqn.parent.javaPackage
    val classNameAction = service.className
    val protoName = service.fqn.protoName

    val descriptors =
      (collectRelevantTypes(service.commandTypes, service.fqn)
        .map(d =>
          s"${d.parent.javaOuterClassname}.getDescriptor()") :+ s"${service.fqn.parent.javaOuterClassname}.getDescriptor()").distinct.sorted

    val imports = generateImports(
      service.commandTypes,
      packageName,
      otherImports = Seq(
        "com.akkaserverless.javasdk.action.ActionCreationContext",
        "com.akkaserverless.javasdk.action.ActionProvider",
        "com.akkaserverless.javasdk.action.ActionOptions",
        "com.akkaserverless.javasdk.impl.action.ActionHandler",
        "com.google.protobuf.Descriptors",
        "java.util.function.Function") ++ service.commandTypes.map(_.descriptorImport))

    s"""$managedComment
      |
      |package $packageName;
      |
      |$imports
      |
      |/**
      | * ${service.providerName} that defines how to register and create the action for
      | * the Protobuf service <code>$protoName</code>.
      | *
      | * Should be used with the <code>register</code> method in {@link com.akkaserverless.javasdk.AkkaServerless}.
      | */
      |public class ${service.providerName} implements ActionProvider<$classNameAction> {
      |
      |  private final Function<ActionCreationContext, $classNameAction> actionFactory;
      |  private final ActionOptions options;
      |
      |  /** Factory method of ${service.providerName} */
      |  public static ${service.providerName} of(Function<ActionCreationContext, $classNameAction> actionFactory) {
      |    return new ${service.providerName}(actionFactory, ActionOptions.defaults());
      |  }
      |
      |  private ${service.providerName}(Function<ActionCreationContext, $classNameAction> actionFactory, ActionOptions options) {
      |    this.actionFactory = actionFactory;
      |    this.options = options;
      |  }
      |
      |  @Override
      |  public final ActionOptions options() {
      |    return options;
      |  }
      |
      |  public final ${service.providerName} withOptions(ActionOptions options) {
      |    return new ${service.providerName}(actionFactory, options);
      |  }
      |
      |  @Override
      |  public final Descriptors.ServiceDescriptor serviceDescriptor() {
      |    return ${service.descriptorObject.name}.getDescriptor().findServiceByName("$protoName");
      |  }
      |
      |  @Override
      |  public final ${service.handlerName} newHandler(ActionCreationContext context) {
      |    return new ${service.handlerName}(actionFactory.apply(context));
      |  }
      |
      |  @Override
      |  public final Descriptors.FileDescriptor[] additionalDescriptors() {
      |    return new Descriptors.FileDescriptor[] {
      |      ${Format.indent(descriptors.mkString(",\n"), 6)}
      |    };
      |  }
      |
      |}
      |""".stripMargin
  }
}
