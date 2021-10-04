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
      service: ModelBuilder.ActionService,
      testSourceDirectory: Path,
      generatedTestSourceDirectory: Path): Iterable[Path] = {
    var generatedFiles: Seq[Path] = Vector.empty
    val packageName = service.fqn.parent.javaPackage
    val className = service.className

    val packagePath = packageAsPath(packageName)
    val testKitPath = generatedTestSourceDirectory.resolve(packagePath.resolve(className + "TestKit.java"))
    testKitPath.getParent.toFile.mkdirs()
    val sourceCode = generateSourceCode(service, packageName, className)
    Files.write(testKitPath, sourceCode.getBytes(Charsets.UTF_8))
    generatedFiles :+= testKitPath

    generatedFiles
  }

  private[codegen] def generateSourceCode(
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
        s"$packageName.$className",
        "com.akkaserverless.javasdk.action.Action",
        "com.akkaserverless.javasdk.action.ActionCreationContext",
        "com.akkaserverless.javasdk.testkit.ActionResult",
        "com.akkaserverless.javasdk.testkit.impl.ActionResultImpl",
        "com.akkaserverless.javasdk.impl.action.ActionEffectImpl",
        "com.akkaserverless.javasdk.testkit.impl.StubActionCreationContext",
        "com.akkaserverless.javasdk.testkit.impl.StubActionContext"))

    val testKitClassName = s"${className}TestKit"

    s"""$managedComment
          |package ${service.fqn.parent.javaPackage};
          |
          |$imports
          |
          |public final class $testKitClassName {
          |
          |  private Function<ActionCreationContext, $className> actionFactory;
          |
          |  private $className createAction() {
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
          |  ${Format.indent(generateServices(service), 2)}
          |
          |}
          |""".stripMargin
  }

  def generateServices(service: ModelBuilder.ActionService): String = {
    require(!service.commands.isEmpty, "empty `commands` not allowed")

    def selectOutput(command: ModelBuilder.Command): String = {
      val parent = command.outputType.parent
      s"${parent.javaPackageOption.getOrElse(parent.protoPackage)}.${filterJavaOuterClassname(
        parent.javaOuterClassnameOption)}${command.outputType.name}"

    }

    def selectInput(command: ModelBuilder.Command): String = {
      val parent = command.inputType.parent
      s"${parent.javaPackageOption.getOrElse(parent.protoPackage)}.${filterJavaOuterClassname(
        parent.javaOuterClassnameOption)}${command.inputType.name}"

    }

    def filterJavaOuterClassname(javaOuterClassnameOption: Option[String]): String = {
      javaOuterClassnameOption match {
        case None => ""
        case Some(value) =>
          if (value == "EmptyProto" || value == "AnyProto") ""
          else value + "."
      }
    }

    service.commands
      .map { command =>
        s"""|public ActionResult<${selectOutput(command)}> ${lowerFirst(command.name)}(${selectInput(
          command)} ${lowerFirst(command.inputType.protoName)}) {
            |  Action.Effect<${selectOutput(command)}> effect = createAction().${lowerFirst(command.name)}(${lowerFirst(
          command.inputType.protoName)});
            |  return interpretEffects(effect);
            |}
            |""".stripMargin + "\n"
      }
      .mkString("")
  }

}
