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

object EventSourcedEntityTestKitGenerator {
  import JavaGeneratorUtils._
  import kalix.codegen.SourceGeneratorUtils._

  def generate(entity: ModelBuilder.EventSourcedEntity, service: ModelBuilder.EntityService): GeneratedFiles = {
    val pkg = entity.messageType.parent
    val className = entity.messageType.name

    GeneratedFiles.Empty
      .addManagedTest(
        File.java(pkg, className + "TestKit", generateSourceCode(service, entity, pkg.javaPackage, className)))
      .addUnmanagedTest(File.java(pkg, className + "Test", generateTestSources(service, entity)))
  }

  private[codegen] def generateSourceCode(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.EventSourcedEntity,
      packageName: String,
      className: String): String = {
    val imports = generateImports(
      allRelevantMessageTypes(service, entity),
      packageName,
      otherImports = Seq(
        "com.google.protobuf.Empty",
        "java.util.ArrayList",
        "java.util.List",
        "java.util.NoSuchElementException",
        "kalix.javasdk.Metadata",
        "kalix.javasdk.eventsourcedentity.EventSourcedEntity",
        "kalix.javasdk.eventsourcedentity.EventSourcedEntityContext",
        "kalix.javasdk.impl.effect.SecondaryEffectImpl",
        "kalix.javasdk.impl.effect.MessageReplyImpl",
        "kalix.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl",
        "kalix.javasdk.testkit.EventSourcedResult",
        "kalix.javasdk.testkit.impl.TestKitEventSourcedEntityContext",
        "kalix.javasdk.testkit.impl.EventSourcedResultImpl",
        "kalix.javasdk.testkit.impl.EventSourcedEntityEffectsRunner",
        "kalix.javasdk.testkit.impl.TestKitEventSourcedEntityCommandContext",
        "kalix.javasdk.testkit.impl.TestKitEventSourcedEntityEventContext",
        "java.util.function.Function",
        "java.util.Optional"))

    val entityClassName = entity.messageType.name
    val stateClassName = entity.state.messageType.fullName
    val testkitClassName = s"${entityClassName}TestKit"

    s"""package ${entity.messageType.parent.javaPackage};
          |
          |${writeImports(imports)}
          |
          |$managedComment
          |
          |/**
          | * TestKit for unit testing $entityClassName
          | */
          |public final class ${testkitClassName} extends EventSourcedEntityEffectsRunner<$stateClassName, Object> {
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
          |  private ${entityClassName} entity;
          |
          |  /** Construction is done through the static $testkitClassName.of-methods */
          |  private ${testkitClassName}(${entityClassName} entity) {
          |     super(entity);
          |     this.entity = entity;
          |  }
          |
          |  public $stateClassName handleEvent($stateClassName state, Object event) {
          |    try {
          |      entity._internalSetEventContext(Optional.of(new TestKitEventSourcedEntityEventContext()));
          |      ${Format.indent(generateHandleEvents(entity.events), 6)}
          |    } finally {
          |      entity._internalSetEventContext(Optional.empty());
          |    }
          |  }
          |
          |  ${Format.indent(generateServices(service), 2)}
          |
          |  ${Format.indent(generateServicesDefault(service), 2)}
          |
          |}
          |""".stripMargin
  }

  def generateServices(service: ModelBuilder.EntityService): String = {
    def selectOutput(command: ModelBuilder.Command): String =
      if (command.outputType.name == "Empty") {
        "Empty"
      } else {
        command.outputType.fullName
      }

    val commands = service.commands.filterNot(_.ignore)
    commands
      .map { command =>
        s"""|public EventSourcedResult<${selectOutput(command)}> ${lowerFirst(command.name)}(${command.inputType.fullName} command, Metadata metadata) {
            |  return interpretEffects(() -> entity.${lowerFirst(command.name)}(getState(), command), metadata);
            |}
            |""".stripMargin + "\n"
      }
      .mkString("")
  }

  def generateServicesDefault(service: ModelBuilder.EntityService): String = {
    def selectOutput(command: ModelBuilder.Command): String =
      if (command.outputType.name == "Empty") {
        "Empty"
      } else {
        command.outputType.fullName
      }

    val commands = service.commands.filterNot(_.ignore)
    commands
      .map { command =>
        s"""|public EventSourcedResult<${selectOutput(command)}> ${lowerFirst(command.name)}(${command.inputType.fullName} command) {
            |  return interpretEffects(() -> entity.${lowerFirst(command.name)}(getState(), command), Metadata.EMPTY);
            |}
            |""".stripMargin + "\n"
      }
      .mkString("")
  }

  //TODO This method should be deleted when the codegen CartHandler.handleEvents gets available
  def generateHandleEvents(events: Iterable[ModelBuilder.Event]): String = {
    require(events.nonEmpty, "empty `events` not allowed")

    val top =
      s"""|if (event instanceof ${events.head.messageType.fullName}) {
          |  return entity.${lowerFirst(events.head.messageType.name)}(state, (${events.head.messageType.fullName}) event);
          |""".stripMargin

    val middle = events.tail.map { event =>
      s"""|} else if (event instanceof ${event.messageType.fullName}) {
          |  return entity.${lowerFirst(
        event.messageType.name)}(state, (${event.messageType.fullName}) event);""".stripMargin
    }

    val bottom =
      s"""
        |} else {
        |  throw new NoSuchElementException("Unknown event type [" + event.getClass() + "]");
        |}""".stripMargin

    top + middle.mkString("\n") + bottom
  }

  def generateTestSources(service: ModelBuilder.EntityService, entity: ModelBuilder.EventSourcedEntity): String = {
    val packageName = entity.messageType.parent.javaPackage
    val commands = service.commands.filterNot(_.ignore)
    val imports = generateImports(
      allRelevantMessageTypes(service, entity),
      packageName,
      otherImports = Seq(
        "com.google.protobuf.Empty",
        "kalix.javasdk.eventsourcedentity.EventSourcedEntity",
        "kalix.javasdk.eventsourcedentity.EventSourcedEntityContext",
        "kalix.javasdk.testkit.EventSourcedResult",
        "org.junit.jupiter.api.Disabled",
        "org.junit.jupiter.api.Test"))

    val entityClassName = entity.messageType.name
    val testkitClassName = s"${entityClassName}TestKit"

    val dummyTestCases = commands.map { command =>
      s"""|@Test
          |@Disabled("to be implemented")
          |public void ${lowerFirst(command.name)}Test() {
          |  $testkitClassName service = $testkitClassName.of(${entityClassName}::new);
          |  // ${command.inputType.name} command = ${command.inputType.name}.newBuilder()...build();
          |  // EventSourcedResult<${command.outputType.name}> result = service.${lowerFirst(command.name)}(command);
          |}
          |
          |""".stripMargin
    }

    s"""package $packageName;
      |
      |${writeImports(imports)}
      |
      |import static org.junit.jupiter.api.Assertions.*;
      |
      |$unmanagedComment
      |
      |public class ${entityClassName}Test {
      |
      |  @Test
      |  @Disabled("to be implemented")
      |  public void exampleTest() {
      |    $testkitClassName service = $testkitClassName.of(${entityClassName}::new);
      |    // // use the testkit to execute a command
      |    // // of events emitted, or a final updated state:
      |    // SomeCommand command = SomeCommand.newBuilder()...build();
      |    // EventSourcedResult<SomeResponse> result = service.someOperation(command);
      |    // // verify the emitted events
      |    // ExpectedEvent actualEvent = result.getNextEventOfType(ExpectedEvent.class);
      |    // assertEquals(expectedEvent, actualEvent);
      |    // // verify the final state after applying the events
      |    // assertEquals(expectedState, service.getState());
      |    // // verify the reply
      |    // SomeReply reply = result.getReply();
      |    // assertEquals(expectedReply, reply);
      |  }
      |
      |  ${Format.indent(dummyTestCases, 2)}
      |
      |}
      |""".stripMargin
  }

}
