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

object ActionTestKitGenerator {
  import JavaGeneratorUtils._
  import kalix.codegen.SourceGeneratorUtils._

  def generate(service: ModelBuilder.ActionService): GeneratedFiles = {
    val pkg = service.messageType.parent
    val className = service.className

    GeneratedFiles.Empty
      .addManagedTest(File.java(pkg, className + "TestKit", generateSourceCode(service)))
      .addUnmanagedTest(File.java(pkg, className + "Test", generateTestSourceCode(service)))
  }

  private[codegen] def generateSourceCode(service: ModelBuilder.ActionService): String = {

    val packageName = service.messageType.parent.javaPackage
    val className = service.className
    val commands = service.commands.filterNot(_.ignore)

    val imports = generateImports(
      commandTypes(commands),
      "",
      otherImports = Seq(
        "java.util.function.Function",
        "java.util.Optional",
        s"$packageName.$className",
        "kalix.javasdk.Metadata",
        "kalix.javasdk.action.Action.Effect",
        "kalix.javasdk.action.ActionCreationContext",
        "kalix.javasdk.testkit.ActionResult",
        "kalix.javasdk.testkit.impl.ActionResultImpl",
        "kalix.javasdk.testkit.impl.TestKitActionContext",
        "kalix.javasdk.testkit.MockRegistry")
        ++ commandStreamedTypes(commands))

    val testKitClassName = s"${className}TestKit"

    s"""package ${service.messageType.parent.javaPackage};
        |
        |${writeImports(imports)}
        |
        |$managedComment
        |
        |public final class $testKitClassName {
        |
        |  private final Function<ActionCreationContext, $className> actionFactory;
        |
        |  private final MockRegistry mockRegistry;
        |
        |  private $className createAction(TestKitActionContext context) {
        |    $className action = actionFactory.apply(context);
        |    action._internalSetActionContext(Optional.of(context));
        |    return action;
        |  }
        |
        |  public static $testKitClassName of(Function<ActionCreationContext, $className> actionFactory) {
        |    return new $testKitClassName(actionFactory, MockRegistry.EMPTY);
        |  }
        |
        |  public static $testKitClassName of(Function<ActionCreationContext, $className> actionFactory, MockRegistry mockRegistry) {
        |    return new $testKitClassName(actionFactory, mockRegistry);
        |  }
        |
        |  private $testKitClassName(Function<ActionCreationContext, $className> actionFactory, MockRegistry mockRegistry) {
        |    this.actionFactory = actionFactory;
        |    this.mockRegistry = mockRegistry;
        |  }
        |
        |  private <E> ActionResult<E> interpretEffects(Effect<E> effect) {
        |    return new ActionResultImpl(effect);
        |  }
        |
        |  ${Format.indent(generateServices(service), 2)}
        |
        |  ${Format.indent(generateServicesDefault(service), 2)}
        |
        |}
        |""".stripMargin
  }

  private[codegen] def generateTestSourceCode(service: ModelBuilder.ActionService): String = {
    val className = service.className
    val packageName = service.messageType.parent.javaPackage
    val commands = service.commands.filterNot(_.ignore)
    val imports = generateImports(
      commandTypes(commands),
      "",
      otherImports = Seq(
        s"$packageName.$className",
        s"${packageName}.${className}TestKit",
        "akka.stream.javadsl.Source",
        "kalix.javasdk.testkit.ActionResult",
        "org.junit.jupiter.api.Disabled",
        "org.junit.jupiter.api.Test",
        "static org.junit.jupiter.api.Assertions.*")
        ++ commandStreamedTypes(commands))

    val testClassName = s"${className}Test"

    s"""package ${service.messageType.parent.javaPackage};
        |
        |${writeImports(imports)}
        |
        |$unmanagedComment
        |
        |public class $testClassName {
        |
        |  @Test
        |  @Disabled("to be implemented")
        |  public void exampleTest() {
        |    ${className}TestKit service = ${className}TestKit.of($className::new);
        |    // // use the testkit to execute a command
        |    // SomeCommand command = SomeCommand.newBuilder()...build();
        |    // ActionResult<SomeResponse> result = service.someOperation(command);
        |    // // verify the reply
        |    // SomeReply reply = result.getReply();
        |    // assertEquals(expectedReply, reply);
        |  }
        |
        |  ${Format.indent(generateTestingServices(service), 2)}
        |
        |}
        |""".stripMargin
  }

  def generateServices(service: ModelBuilder.ActionService): String = {
    val commands = service.commands.filterNot(_.ignore)

    commands
      .map { command =>
        if (command.handleDeletes) {
          s"""|public ${selectOutputResult(command)} ${lowerFirst(command.name)}(Metadata metadata) {
          |  TestKitActionContext context = new TestKitActionContext(metadata, mockRegistry);
          |  ${selectOutputEffect(command)} effect = createAction(context).${lowerFirst(command.name)}();
          |  return ${selectOutputReturn(command)}
          |}
          |""".stripMargin + "\n"
        } else {
          s"""|public ${selectOutputResult(command)} ${lowerFirst(command.name)}(${selectInputType(
            command)} ${lowerFirst(command.inputType.protoName)}, Metadata metadata) {
              |  TestKitActionContext context = new TestKitActionContext(metadata, mockRegistry);
              |  ${selectOutputEffect(command)} effect = createAction(context).${lowerFirst(command.name)}(${lowerFirst(
            command.inputType.protoName)});
              |  return ${selectOutputReturn(command)}
              |}
              |""".stripMargin + "\n"
        }
      }
      .mkString("")
  }

  /**
   * Leveraging `generateServices` by setting default Metadata as Metadata.EMPTY
   */
  def generateServicesDefault(service: ModelBuilder.ActionService): String = {
    val commands = service.commands.filterNot(_.ignore)

    commands
      .map { command =>
        if (command.handleDeletes) {
          s"""|public ${selectOutputResult(command)} ${lowerFirst(command.name)}() {
              |  return ${lowerFirst(command.name)}(Metadata.EMPTY);
              |}
              |""".stripMargin + "\n"
        } else {
          s"""|public ${selectOutputResult(command)} ${lowerFirst(command.name)}(${selectInputType(
            command)} ${lowerFirst(command.inputType.protoName)}) {
              |  return ${lowerFirst(command.name)}(${lowerFirst(command.inputType.protoName)}, Metadata.EMPTY);
              |}
              |""".stripMargin + "\n"
        }
      }
      .mkString("")
  }

  def generateTestingServices(service: ModelBuilder.ActionService): String = {
    val commands = service.commands.filterNot(_.ignore)

    commands
      .map { command =>
        s"""|@Test
            |@Disabled("to be implemented")
            |public void ${lowerFirst(command.name)}Test() {
            |  ${service.className}TestKit testKit = ${service.className}TestKit.of(${service.className}::new);""".stripMargin +
        (if (command.isUnary || command.isStreamOut) {
           if (command.handleDeletes) {
             s"""
               |  // ${selectOutputResult(command)} result = testKit.${lowerFirst(command.name)}();
               |}
               |
               |""".stripMargin
           } else {
             s"""
               |  // ${selectOutputResult(command)} result = testKit.${lowerFirst(command.name)}(${command.inputType.fullName}.newBuilder()...build());
               |}
               |
               |""".stripMargin
           }
         } else {
           s"""
            |  // ${selectOutputResult(command)} result = testKit.${lowerFirst(command.name)}(Source.single(${command.inputType.fullName}.newBuilder()...build()));
            |}
            |
            |""".stripMargin
         })
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
      s"Source<Effect<${command.outputType.fullName}>, akka.NotUsed>"
    else s"Effect<${command.outputType.fullName}>"
  }

  def selectOutputReturn(command: ModelBuilder.Command): String = {
    if (command.streamedOutput) "effect.map(e -> interpretEffects(e));"
    else "interpretEffects(effect);"
  }

  def selectInputType(command: ModelBuilder.Command): String = {
    if (command.streamedInput) s"Source<${command.inputType.fullName}, akka.NotUsed>"
    else command.inputType.fullName
  }

  def commandStreamedTypes(commands: Iterable[ModelBuilder.Command]): Seq[String] = {
    if (commands.exists(c => c.streamedInput || c.streamedOutput)) Seq("akka.stream.javadsl.Source", "akka.NotUsed")
    else Nil
  }

}
