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

package com.lightbend.akkasls.codegen.java

import com.google.common.base.Charsets
import com.lightbend.akkasls.codegen.ModelBuilder
import com.lightbend.akkasls.codegen.Syntax
import com.lightbend.akkasls.codegen.java.EntityServiceSourceGenerator.generateImports
import com.lightbend.akkasls.codegen.java.SourceGenerator.generatedCodeCommentString
import com.lightbend.akkasls.codegen.java.SourceGenerator.lowerFirst
import com.lightbend.akkasls.codegen.java.SourceGenerator.managedCodeCommentString
import com.lightbend.akkasls.codegen.java.SourceGenerator.packageAsPath

import java.nio.file.Files
import java.nio.file.Path

object ValueEntityTestKitGenerator {

  def generate(entity: ModelBuilder.ValueEntity,
               service: ModelBuilder.EntityService,
               testSourceDirectory: Path,
               generatedSourceDirectory: Path): Iterable[Path] = {
    var generatedFiles: Seq[Path] = Vector.empty
    val packageName = entity.fqn.parent.javaPackage
    val className = entity.fqn.name
    val sourceCode = generateSourceCode(service, entity, packageName)

    val packagePath = packageAsPath(packageName)
    val testKitPath = generatedSourceDirectory.resolve(packagePath.resolve(className + "TestKit.java"))

    testKitPath.getParent.toFile.mkdirs()
    Files.write(testKitPath, sourceCode.getBytes(Charsets.UTF_8))
    generatedFiles :+= testKitPath

    val testFilePath = testSourceDirectory.resolve(packagePath.resolve(className + "Test.java"))
    if (!testFilePath.toFile.exists()) {
      testFilePath.getParent.toFile.mkdirs()
      Files.write(testFilePath, generateTestSources(service, entity, packageName).getBytes(Charsets.UTF_8))
      generatedFiles :+= testFilePath
    }

    generatedFiles
  }

  private[codegen] def generateSourceCode(service: ModelBuilder.EntityService,
                                          entity: ModelBuilder.ValueEntity,
                                          packageName: String): String = {
    val imports = generateImports(
      service.commands,
      entity.state,
      packageName,
      otherImports = Seq(
        "com.google.protobuf.Empty",
        "java.util.ArrayList",
        "java.util.List",
        "java.util.NoSuchElementException",
        "scala.jdk.javaapi.CollectionConverters",
        "com.akkaserverless.javasdk.valueentity.ValueEntity",
        "com.akkaserverless.javasdk.impl.effect.SecondaryEffectImpl",
        "com.akkaserverless.javasdk.impl.effect.MessageReplyImpl",
        "com.akkaserverless.javasdk.impl.valueentity.ValueEntityEffectImpl",
        "com.akkaserverless.javasdk.testkit.ValueEntityResult",
        "com.akkaserverless.javasdk.testkit.impl.ValueEntityResultImpl",
        "com.akkaserverless.javasdk.valueentity.ValueEntityContext",
        "com.akkaserverless.javasdk.testkit.impl.AkkaServerlessTestKitHelper",
        "com.akkaserverless.javasdk.testkit.impl.TestKitValueEntityContext",
        "java.util.function.Function"
      )
    )

    val domainClassName = entity.fqn.parent.javaOuterClassname
    val entityClassName = entity.fqn.name
    val entityStateName = entity.state.fqn.name
    val stateClassName = s"${domainClassName}.${entityStateName}"

    val testkitClassName = s"${entityClassName}TestKit"

    s"""$managedCodeCommentString
       |package ${entity.fqn.parent.pkg};
       |
       |$imports
       |
       |/**
       | * TestKit for unit testing $entityClassName
       | */
       |public final class ${testkitClassName} {
       |
       |  private $stateClassName state;
       |  private $entityClassName entity;
       |
       |  /**
       |   * Create a testkit instance of $entityClassName
       |   * @param entityFactory A function that creates a $entityClassName based on the given ValueEntityContext,
       |   *                      a default entity id is used.
       |   */
       |  public static $testkitClassName of(Function<ValueEntityContext, $entityClassName> entityFactory) {
       |    return of("testkit-entity-id", entityFactory);
       |  }
       |
       |  /**
       |   * Create a testkit instance of $entityClassName with a specific entity id.
       |   */
       |  public static $testkitClassName of(String entityId, Function<ValueEntityContext, $entityClassName> entityFactory) {
       |    return new $testkitClassName(entityFactory.apply(new TestKitValueEntityContext(entityId)));
       |  }
       |
       |  /** Construction is done through the static $testkitClassName.of-methods */
       |  private ${testkitClassName}($entityClassName entity) {
       |    this.state = entity.emptyState();
       |    this.entity = entity;
       |  }
       |
       |  public $testkitClassName($entityClassName entity, $stateClassName state) {
       |    this.state = state;
       |    this.entity = entity;
       |  }
       |
       |  /**
       |   * @return The current state of the $entityClassName under test
       |   */
       |  public $stateClassName getState() {
       |    return state;
       |  }
       |
       |  private <Reply> ValueEntityResult<Reply> interpretEffects(ValueEntity.Effect<Reply> effect) {
       |    @SuppressWarnings("unchecked")
       |    ValueEntityResultImpl<Reply> result = new ValueEntityResultImpl<>(effect);
       |    if (result.stateWasUpdated()) {
       |      this.state = ($stateClassName) result.getUpdatedState();
       |    }
       |    return result;
       |  }
       |
       |  ${Syntax.indent(generateServices(service), 2)}
       |}""".stripMargin
  }

  def generateServices(service: ModelBuilder.EntityService): String = {
    val apiClassName = service.fqn.parent.javaOuterClassname

    def selectOutput(command: ModelBuilder.Command): String =
      if (command.outputType.name == "Empty") {
        "Empty"
      } else {
        apiClassName + "." + command.outputType.name
      }

    service.commands
      .map { command =>
        val output = selectOutput(command)
        s"""|public ValueEntityResult<$output> ${lowerFirst(command.fqn.name)}(${apiClassName}.${command.inputType.name} ${lowerFirst(
             command.inputType.name
           )}) {
       |  ValueEntity.Effect<$output> effect = entity.${lowerFirst(command.fqn.name)}(state, ${lowerFirst(
             command.inputType.name
           )});
       |  return interpretEffects(effect);
       |}""".stripMargin
      }
      .mkString("\n\n")
  }

  def generateTestSources(service: ModelBuilder.EntityService,
                          entity: ModelBuilder.ValueEntity,
                          packageName: String): String = {
    val imports = generateImports(
      service.commands,
      entity.state,
      packageName,
      otherImports = Seq(
        "com.google.protobuf.Empty",
        "java.util.ArrayList",
        "java.util.List",
        "java.util.NoSuchElementException",
        "scala.jdk.javaapi.CollectionConverters",
        "com.akkaserverless.javasdk.valueentity.ValueEntity",
        "com.akkaserverless.javasdk.testkit.ValueEntityResult",
        "org.junit.Test"
      )
    )

    val entityClassName = entity.fqn.name
    val testkitClassName = s"${entityClassName}TestKit"

    val dummyTestCases = service.commands.map { command =>
      s"""|@Test
          |public void ${lowerFirst(command.fqn.name)}Test() {
          |  $testkitClassName testKit = $testkitClassName.of(${entityClassName}::new);
          |  // ValueEntityResult<${command.outputType.name}> result = testKit.${lowerFirst(command.fqn.name)}(${command.inputType.name}.newBuilder()...build());
          |}
          |
          |""".stripMargin
    }

    s"""$generatedCodeCommentString
       |package ${entity.fqn.parent.pkg};
       |
       |$imports
       |
       |import static org.junit.Assert.*;
       |
       |public class ${entityClassName}Test {
       |
       |  @Test
       |  public void exampleTest() {
       |    $testkitClassName testKit = $testkitClassName.of(${entityClassName}::new);
       |    // use the testkit to execute a command
       |    // of events emitted, or a final updated state:
       |    // ValueEntityResult<SomeResponse> result = testKit.someOperation(SomeRequest);
       |    // verify the response
       |    // SomeResponse actualResponse = result.getReply();
       |    // assertEquals(expectedResponse, actualResponse);
       |    // verify the final state after the command
       |    // assertEquals(expectedState, testKit.getState());
       |  }
       |
       |  ${Syntax.indent(dummyTestCases, 2)}
       |
       |}
       |""".stripMargin
  }

}
