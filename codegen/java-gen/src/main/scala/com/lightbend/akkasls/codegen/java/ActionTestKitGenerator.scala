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
import _root_.java.nio.file.{ Files, Path }

object ActionTestKitGenerator {
  import com.lightbend.akkasls.codegen.SourceGeneratorUtils._

  def generate(
      entity: ModelBuilder.EventSourcedEntity,
      service: ModelBuilder.ActionService,
      testSourceDirectory: Path,
      generatedSourceDirectory: Path): Iterable[Path] = {
    var generatedFiles: Seq[Path] = Vector.empty
    val packageName = service.fqn.parent.javaPackage
    val className = service.fqn.name

    val packagePath = packageAsPath(packageName)
    val testKitPath = generatedSourceDirectory.resolve(packagePath.resolve(className + "TestKit.java"))
    testKitPath.getParent.toFile.mkdirs()
    val sourceCode = generateSourceCode(entity, service, packageName, className)
    Files.write(testKitPath, sourceCode.getBytes(Charsets.UTF_8))
    generatedFiles :+= testKitPath

    generatedFiles
  }

  private[codegen] def generateSourceCode(
      entity: ModelBuilder.EventSourcedEntity,
      service: ModelBuilder.ActionService,
      packageName: String,
      className: String): String = {
    val imports = generateImports(
      commandTypes(service.commands),
      "",
      otherImports = Seq(
        "java.util.ArrayList",
        "java.util.List",
        "java.util.function.Function",
        "java.util.Optional",
        "com.akkaserverless.javasdk.action.Action",
        "com.akkaserverless.javasdk.action.ActionCreationContext",
        "com.akkaserverless.javasdk.testkit.impl.ActionResultImpl",
        "com.akkaserverless.javasdk.impl.action.ActionEffectImpl",
        "com.example.actions.CounterJournalToTopicAction",
        "com.example.actions.CounterTopicApi",
        "com.akkaserverless.javasdk.testkit.impl.StubActionCreationContext",
        "com.akkaserverless.javasdk.testkit.impl.StubActionContext"))

    val testKitClassName = s"${className}TestKit"

    println(packageName)

    s"""$managedComment
          |package ${service.fqn.parent.javaPackage};
          |
          |$imports
          |
          |public final class $testKitClassName {
          |
          |  private Function<ActionCreationContext, $className> actionFactory;
          |
          |  private CounterJournalToTopicAction createAction() {
          |    $className action = actionFactory.apply(new StubActionCreationContext());
          |    action._internalSetActionContext(Optional.of(new StubActionContext()));
          |    return action;
          |  };
          |
          |  public static $testKitClassName of(Function<ActionCreationContext, $className> actionFactory) {
          |    return new $testKitClassName(actionFactory);
          |  }
          |
          |  private $testKitClassName(Function<ActionCreationContext, $className> actionFactory) {
          |    this.actionFactory = actionFactory;
          |  }
          |
          |  private <E> ActionResult<E> interpretEffects(Action.Effect<E> effect) {
          |    return new ActionResultImpl(effect);
          |  }
          |
          |  ${Format.indent(generateServices(service, entity), 2)}
          |
          |}
          |""".stripMargin
  }

  def generateServices(service: ModelBuilder.ActionService, entity: ModelBuilder.EventSourcedEntity): String = {
    require(!service.commands.isEmpty, "empty `commands` not allowed")

    def selectOutput(command: ModelBuilder.Command): String =
      if (command.outputType.name == "Empty") {
        "Empty"
      } else {
        command.outputType.fullName
      }

    val domainOuterClass = entity.fqn.parent.name

    service.commands
      .map { command =>
        s"""|public ActionResult<${selectOutput(command)}> ${lowerFirst(command.name)}(${domainOuterClass}.${command.inputType.protoName} event) {
            |  Action.Effect<${selectOutput(command)}> effect = createAction().${lowerFirst(command.name)}(event);
            |  return interpretEffects(effect);
            |}
            |""".stripMargin + "\n"
      }
      .mkString("")
  }

}
