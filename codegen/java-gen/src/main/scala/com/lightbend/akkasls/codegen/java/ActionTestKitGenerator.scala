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
    val sourceCode = generateSourceCode(service)
    Files.write(testKitPath, sourceCode.getBytes(Charsets.UTF_8))
    generatedFiles :+= testKitPath

    val testFilePath = testSourceDirectory.resolve(packagePath.resolve(className + "Test.java"))
    if (!testFilePath.toFile.exists()) {
      testFilePath.getParent.toFile.mkdirs()
      Files.write(testFilePath, generateTestSourceCode(service).getBytes(Charsets.UTF_8))
      generatedFiles :+= testFilePath
    }

    generatedFiles
  }

  private[codegen] def generateSourceCode(service: ModelBuilder.ActionService): String = {

    val packageName = service.fqn.parent.javaPackage
    val className = service.className

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
        "com.akkaserverless.javasdk.testkit.impl.StubActionContext")
        ++ commandStreamedTypes(service.commands))

    val testKitClassName = s"${className}TestKit"

    s"""package ${service.fqn.parent.javaPackage};
          |
          |$imports
          |
          |$managedComment
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

  private[codegen] def generateTestSourceCode(service: ModelBuilder.ActionService): String = {
    val className = service.className
    val packageName = service.fqn.parent.javaPackage
    val imports = generateImports(
      commandTypes(service.commands),
      "",
      otherImports = Seq(
        s"$packageName.$className",
        s"${packageName}.${className}TestKit",
        "com.akkaserverless.javasdk.testkit.ActionResult",
        "org.junit.Test",
        "static org.junit.Assert.*")
        ++ commandStreamedTypes(service.commands))

    val testClassName = s"${className}Test"

    s"""$unmanagedComment
          |package ${service.fqn.parent.javaPackage};
          |
          |$imports
          |
          |public class $testClassName {
          |
          |  @Test
          |  public void exampleTest() {
          |    ${className}TestKit testKit = ${className}TestKit.of($className::new);
          |    // use the testkit to execute a command
          |    // ActionResult<SomeResponse> result = testKit.someOperation(SomeRequest);
          |    // verify the response
          |    // SomeResponse actualResponse = result.getReply();
          |    // assertEquals(expectedResponse, actualResponse);
          |  }
          |
          |  ${Format.indent(generateTestingServices(service), 2)}
          |
          |}
          |""".stripMargin
  }

  def generateServices(service: ModelBuilder.ActionService): String = {
    require(!service.commands.isEmpty, "empty `commands` not allowed")

    service.commands
      .map { command =>
        s"""|public ${selectOutputResult(command)} ${lowerFirst(command.name)}(${selectInput(command)} ${lowerFirst(
          command.inputType.protoName)}) {
            |  ${selectOutputEffect(command)} effect = createAction().${lowerFirst(command.name)}(${lowerFirst(
          command.inputType.protoName)});
            |  return ${selectOutputReturn(command)}
            |}
            |""".stripMargin + "\n"
      }
      .mkString("")
  }

  def generateTestingServices(service: ModelBuilder.ActionService): String = {
    service.commands
      .map { command =>
        s"""|@Test
            |public void ${lowerFirst(command.name)}Test() {
            |  ${service.className}TestKit testKit = ${service.className}TestKit.of(${service.className}::new);
            |  // ${selectOutputResult(command)} result = testKit.${lowerFirst(command.name)}(${selectInput(command)}.newBuilder()...build());
            |}
            |""".stripMargin + "\n"
      }
      .mkString("")
  }

  def selectOutputResult(command: ModelBuilder.Command): String = {
    if (command.streamedOutput)
      s"Source<ActionResult<${command.outputType.fullName}>, akka.NotUsed>"
    else s"ActionResult<${command.outputType.fullName}>"
  }

  def selectOutputEffect(command: ModelBuilder.Command): String = {
    if (command.streamedOutput)
      s"Source<Action.Effect<${command.outputType.fullName}>, akka.NotUsed>"
    else s"Action.Effect<${command.outputType.fullName}>"
  }

  def selectOutputReturn(command: ModelBuilder.Command): String = {
    if (command.streamedOutput) "effect.map(e -> interpretEffects(e));"
    else "interpretEffects(effect);"
  }

  def selectInput(command: ModelBuilder.Command): String = {
    if (command.streamedInput) s"Source<${command.inputType.fullName}, akka.NotUsed>"
    else command.inputType.fullName
  }

  def commandStreamedTypes(commands: Iterable[ModelBuilder.Command]): Seq[String] = {
    if (commands.exists(c => c.streamedInput || c.streamedOutput)) Seq("akka.stream.javadsl.Source", "akka.NotUsed")
    else Nil
  }

}
