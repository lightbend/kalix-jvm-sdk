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

object EventSourcedEntityTestKitGenerator {
  import com.lightbend.akkasls.codegen.SourceGeneratorUtils._

  def generate(
      entity: ModelBuilder.EventSourcedEntity,
      service: ModelBuilder.EntityService,
      testSourceDirectory: Path,
      generatedSourceDirectory: Path): Iterable[Path] = {
    var generatedFiles: Seq[Path] = Vector.empty
    val packageName = entity.fqn.parent.javaPackage
    val className = entity.fqn.name

    val packagePath = packageAsPath(packageName)
    val testKitPath = generatedSourceDirectory.resolve(packagePath.resolve(className + "TestKit.java"))
    testKitPath.getParent.toFile.mkdirs()
    val sourceCode = generateSourceCode(service, entity, packageName, className)
    Files.write(testKitPath, sourceCode.getBytes(Charsets.UTF_8))
    generatedFiles :+= testKitPath

    val testFilePath = testSourceDirectory.resolve(packagePath.resolve(className + "Test.java"))
    if (!testFilePath.toFile.exists()) {
      testFilePath.getParent.toFile.mkdirs()
      Files.write(testFilePath, generateTestSources(service, entity).getBytes(Charsets.UTF_8))
      generatedFiles :+= testFilePath
    }

    generatedFiles
  }

  private[codegen] def generateSourceCode(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.EventSourcedEntity,
      packageName: String,
      className: String): String = {
    val imports = generateCommandImports(
      service.commands,
      entity.state,
      packageName,
      otherImports = Seq(
        "com.google.protobuf.Empty",
        "java.util.ArrayList",
        "java.util.List",
        "java.util.NoSuchElementException",
        "scala.jdk.javaapi.CollectionConverters",
        "com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity",
        "com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityContext",
        "com.akkaserverless.javasdk.impl.effect.SecondaryEffectImpl",
        "com.akkaserverless.javasdk.impl.effect.MessageReplyImpl",
        "com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl",
        "com.akkaserverless.javasdk.testkit.EventSourcedResult",
        "com.akkaserverless.javasdk.testkit.impl.TestKitEventSourcedEntityContext",
        "com.akkaserverless.javasdk.testkit.impl.EventSourcedResultImpl",
        "java.util.function.Function"))

    val entityClassName = entity.fqn.name
    val stateClassName = entity.state.fqn.fullName
    val testkitClassName = s"${entityClassName}TestKit"

    s"""$managedComment
          |package ${entity.fqn.parent.javaPackage};
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
          |  private List<Object> events = new ArrayList<Object>();
          |
          |  /**
          |   * Create a testkit instance of $entityClassName
          |   * @param entityFactory A function that creates a $entityClassName based on the given EventSourcedEntityContext,
          |   *                      a default entity id is used.
          |   */
          |  public static $testkitClassName of(Function<EventSourcedEntityContext, $entityClassName> entityFactory) {
          |    return of("testkit-entity-id", entityFactory);
          |  }
          |
          |  /**
          |   * Create a testkit instance of $entityClassName with a specific entity id.
          |   */
          |  public static $testkitClassName of(String entityId, Function<EventSourcedEntityContext, $entityClassName> entityFactory) {
          |    return new $testkitClassName(entityFactory.apply(new TestKitEventSourcedEntityContext(entityId)));
          |  }
          |
          |  /** Construction is done through the static $testkitClassName.of-methods */
          |  private ${testkitClassName}(${entityClassName} entity) {
          |    this.state = entity.emptyState();
          |    this.entity = entity;
          |  }
          |
          |  public ${testkitClassName}(${entityClassName} entity, $stateClassName state) {
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
          |  /**
          |   * @return All events that has been emitted by command handlers since the creation of this testkit.
          |   *         Individual sets of events from a single command handler invokation can be found in the
          |   *         Result from calling it.
          |   */
          |  public List<Object> getAllEvents() {
          |    return this.events;
          |  }
          |
          |  private $stateClassName handleEvent($stateClassName state, Object event) {
          |    ${Format.indent(generateHandleEvents(entity.events), 4)}
          |  }
          |
          |  @SuppressWarnings("unchecked")
          |  private <Reply> EventSourcedResult<Reply> interpretEffects(EventSourcedEntity.Effect<Reply> effect) {
          |    List<Object> events = EventSourcedResultImpl.eventsOf(effect);
          |    this.events.addAll(events);
          |    for(Object e: events) {
          |      this.state = handleEvent(state,e);
          |    }
          |    return new EventSourcedResultImpl(effect, state);
          |  }
          |
          |  ${Format.indent(generateServices(service), 2)}
          |}
          |""".stripMargin
  }

  def generateServices(service: ModelBuilder.EntityService): String = {
    require(!service.commands.isEmpty, "empty `commands` not allowed")

    def selectOutput(command: ModelBuilder.Command): String =
      if (command.outputType.name == "Empty") {
        "Empty"
      } else {
        command.outputType.fullName
      }

    service.commands
      .map { command =>
        s"""|public EventSourcedResult<${selectOutput(command)}> ${lowerFirst(command.name)}(${command.inputType.fullName} command) {
            |  EventSourcedEntity.Effect<${selectOutput(command)}> effect = entity.${lowerFirst(command.name)}(state, command);
            |  return interpretEffects(effect);
            |}
            |""".stripMargin + "\n"
      }
      .mkString("")
  }

  //TODO This method should be deleted when the codegen CartHandler.handleEvents gets available
  def generateHandleEvents(events: Iterable[ModelBuilder.Event]): String = {
    require(events.nonEmpty, "empty `events` not allowed")

    val top =
      s"""|if (event instanceof ${events.head.fqn.fullName}) {
          |  return entity.${lowerFirst(events.head.fqn.name)}(state, (${events.head.fqn.fullName}) event);
          |""".stripMargin

    val middle = events.tail.map { event =>
      s"""|} else if (event instanceof ${event.fqn.fullName}) {
          |  return entity.${lowerFirst(event.fqn.name)}(state, (${event.fqn.fullName}) event);""".stripMargin
    }

    val bottom =
      s"""
        |} else {
        |  throw new NoSuchElementException("Unknown event type [" + event.getClass() + "]");
        |}""".stripMargin

    top + middle.mkString("\n") + bottom
  }

  def generateTestSources(service: ModelBuilder.EntityService, entity: ModelBuilder.EventSourcedEntity): String = {
    val packageName = entity.fqn.parent.javaPackage
    val imports = generateCommandImports(
      service.commands,
      entity.state,
      packageName,
      otherImports = Seq(
        "com.google.protobuf.Empty",
        "java.util.ArrayList",
        "java.util.List",
        "java.util.NoSuchElementException",
        "scala.jdk.javaapi.CollectionConverters",
        "com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity",
        "com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityContext",
        "com.akkaserverless.javasdk.testkit.EventSourcedResult",
        "org.junit.Test"))

    val entityClassName = entity.fqn.name
    val testkitClassName = s"${entityClassName}TestKit"

    val dummyTestCases = service.commands.map { command =>
      s"""|@Test
          |public void ${lowerFirst(command.name)}Test() {
          |  $testkitClassName testKit = $testkitClassName.of(${entityClassName}::new);
          |  // EventSourcedResult<${command.outputType.name}> result = testKit.${lowerFirst(command.name)}(${command.inputType.name}.newBuilder()...build());
          |}
          |
          |""".stripMargin
    }

    s"""$unmanagedComment
      |package $packageName;
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
      |    // EventSourcedResult<SomeResponse> result = testKit.someOperation(SomeRequest);
      |    // verify the emitted events
      |    // ExpectedEvent actualEvent = result.getNextEventOfType(ExpectedEvent.class);
      |    // assertEquals(expectedEvent, actualEvent)
      |    // verify the final state after applying the events
      |    // assertEquals(expectedState, testKit.getState());
      |    // verify the response
      |    // SomeResponse actualResponse = result.getReply();
      |    // assertEquals(expectedResponse, actualResponse);
      |  }
      |
      |  ${Format.indent(dummyTestCases, 2)}
      |
      |}
      |""".stripMargin
  }

}
