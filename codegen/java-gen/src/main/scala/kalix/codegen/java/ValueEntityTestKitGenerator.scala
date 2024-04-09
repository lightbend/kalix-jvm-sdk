/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.codegen.java

import kalix.codegen.File
import kalix.codegen.Format
import kalix.codegen.GeneratedFiles
import kalix.codegen.ModelBuilder

object ValueEntityTestKitGenerator {
  import kalix.codegen.SourceGeneratorUtils._
  import JavaGeneratorUtils._

  def generate(entity: ModelBuilder.ValueEntity, service: ModelBuilder.EntityService): GeneratedFiles = {
    val pkg = entity.messageType.parent
    val className = entity.messageType.name

    GeneratedFiles.Empty
      .addManagedTest(File.java(pkg, className + "TestKit", generateSourceCode(service, entity, pkg.javaPackage)))
      .addUnmanagedTest(File.java(pkg, className + "Test", generateTestSources(service, entity, pkg.javaPackage)))
  }

  private[codegen] def generateSourceCode(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.ValueEntity,
      packageName: String): String = {

    val imports = generateImports(
      allRelevantMessageTypes(service, entity),
      packageName,
      otherImports = Seq(
        "com.google.protobuf.Empty",
        "kalix.javasdk.Metadata",
        "java.util.Optional",
        "kalix.javasdk.valueentity.ValueEntity",
        "kalix.javasdk.impl.effect.SecondaryEffectImpl",
        "kalix.javasdk.impl.effect.MessageReplyImpl",
        "kalix.javasdk.impl.valueentity.ValueEntityEffectImpl",
        "kalix.javasdk.testkit.ValueEntityResult",
        "kalix.javasdk.testkit.impl.ValueEntityResultImpl",
        "kalix.javasdk.valueentity.ValueEntityContext",
        "kalix.javasdk.testkit.impl.TestKitValueEntityContext",
        "kalix.javasdk.testkit.impl.TestKitValueEntityCommandContext",
        "java.util.function.Function"))

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
       |public final class ${testkitClassName} {
       |
       |  private $stateClassName state;
       |  private final $stateClassName emptyState;
       |  private final $entityClassName entity;
       |  private final String entityId;
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
       |    return new $testkitClassName(entityFactory.apply(new TestKitValueEntityContext(entityId)), entityId);
       |  }
       |
       |  /** Construction is done through the static $testkitClassName.of-methods */
       |  private ${testkitClassName}($entityClassName entity, String entityId) {
       |    this.entityId = entityId;
       |    this.state = entity.emptyState();
       |    this.emptyState = state;
       |    this.entity = entity;
       |  }
       |
       |  private $testkitClassName($entityClassName entity, String entityId, $stateClassName state) {
       |    this.entityId = entityId;
       |    this.state = state;
       |    this.emptyState = state;
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
       |    } else if (result.stateWasDeleted()) {
       |      this.state = emptyState;
       |    }
       |    return result;
       |  }
       |
       |  ${Format.indent(generateServices(service), 2)}
       |
       |  ${Format.indent(generateServicesDefault(service), 2)}
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

    service.commands
      .map { command =>
        val output = selectOutput(command)
        s"""|public ValueEntityResult<$output> ${lowerFirst(command.name)}(${command.inputType.fullName} ${lowerFirst(
          command.inputType.name)}, Metadata metadata) {
       |  entity._internalSetCommandContext(Optional.of(new TestKitValueEntityCommandContext(entityId, metadata)));
       |  entity._internalSetCurrentState(state);
       |  ValueEntity.Effect<$output> effect = entity.${lowerFirst(command.name)}(state, ${lowerFirst(
          command.inputType.name)});
       |  return interpretEffects(effect);
       |}""".stripMargin
      }
      .mkString("\n\n")
  }

  def generateServicesDefault(service: ModelBuilder.EntityService): String = {

    def selectOutput(command: ModelBuilder.Command): String =
      if (command.outputType.name == "Empty") {
        "Empty"
      } else {
        command.outputType.fullName
      }

    service.commands
      .map { command =>
        val output = selectOutput(command)
        s"""|public ValueEntityResult<$output> ${lowerFirst(command.name)}(${command.inputType.fullName} ${lowerFirst(
          command.inputType.name)}) {
       |  entity ._internalSetCommandContext(Optional.of(new TestKitValueEntityCommandContext(entityId, Metadata.EMPTY)));
       |  ValueEntity.Effect<$output> effect = entity.${lowerFirst(command.name)}(state, ${lowerFirst(
          command.inputType.name)});
       |  return interpretEffects(effect);
       |}""".stripMargin
      }
      .mkString("\n\n")
  }

  def generateTestSources(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.ValueEntity,
      packageName: String): String = {

    val imports = generateImports(
      allRelevantMessageTypes(service, entity),
      packageName,
      otherImports = Seq(
        "com.google.protobuf.Empty",
        "kalix.javasdk.valueentity.ValueEntity",
        "kalix.javasdk.testkit.ValueEntityResult",
        "org.junit.jupiter.api.Disabled",
        "org.junit.jupiter.api.Test"))

    val entityClassName = entity.messageType.name
    val testkitClassName = s"${entityClassName}TestKit"

    val dummyTestCases = service.commands.map { command =>
      s"""|@Test
          |@Disabled("to be implemented")
          |public void ${lowerFirst(command.name)}Test() {
          |  $testkitClassName service = $testkitClassName.of(${entityClassName}::new);
          |  // ${command.inputType.name} command = ${command.inputType.name}.newBuilder()...build();
          |  // ValueEntityResult<${command.outputType.name}> result = service.${lowerFirst(command.name)}(command);
          |}
          |
          |""".stripMargin
    }

    s"""package ${entity.messageType.parent.javaPackage};
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
       |    // ValueEntityResult<SomeResponse> result = service.someOperation(command);
       |    // // verify the reply
       |    // SomeReply reply = result.getReply();
       |    // assertEquals(expectedReply, reply);
       |    // // verify the final state after the command
       |    // assertEquals(expectedState, service.getState());
       |  }
       |
       |  ${Format.indent(dummyTestCases, 2)}
       |
       |}
       |""".stripMargin
  }

}
