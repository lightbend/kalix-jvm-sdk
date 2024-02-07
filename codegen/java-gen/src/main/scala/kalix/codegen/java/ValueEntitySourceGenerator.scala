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

package kalix.codegen.java

import kalix.codegen.Format
import kalix.codegen.ModelBuilder
import kalix.codegen.ProtoMessageType

object ValueEntitySourceGenerator {
  import JavaGeneratorUtils._
  import kalix.codegen.SourceGeneratorUtils._

  private[codegen] def valueEntitySource(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.ValueEntity,
      packageName: String,
      className: String): String = {

    val imports = generateImports(
      allRelevantMessageTypes(service, entity),
      packageName,
      otherImports = Seq("kalix.javasdk.valueentity.ValueEntityContext"))

    val stateType = entity.state.messageType.fullName

    val methods = service.commands.map { cmd =>
      val methodName = cmd.name
      val inputType = cmd.inputType.fullName
      val outputType = qualifiedType(cmd.outputType)

      s"""|@Override
          |public Effect<$outputType> ${lowerFirst(methodName)}($stateType currentState, $inputType ${lowerFirst(
        cmd.inputType.name)}) {
          |  return effects().error("The command handler for `$methodName` is not implemented, yet");
          |}
          |""".stripMargin
    }

    s"""package $packageName;
        |
        |${writeImports(imports)}
        |
        |${unmanagedComment(Right(entity))}
        |
        |public class $className extends Abstract$className {
        |  @SuppressWarnings("unused")
        |  private final String entityId;
        |
        |  public $className(ValueEntityContext context) {
        |    this.entityId = context.entityId();
        |  }
        |
        |  @Override
        |  public $stateType emptyState() {
        |    throw new UnsupportedOperationException("Not implemented yet, replace with your empty entity state");
        |  }
        |
        |  ${Format.indent(methods, num = 2)}
        |}
        |""".stripMargin
  }

  private[codegen] def valueEntityRouter(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.ValueEntity,
      packageName: String,
      className: String): String = {

    val imports = generateImports(
      allRelevantMessageTypes(service, entity),
      packageName,
      otherImports = Seq(
        "kalix.javasdk.valueentity.CommandContext",
        "kalix.javasdk.valueentity.ValueEntity",
        "kalix.javasdk.impl.valueentity.ValueEntityRouter"))

    val stateType = entity.state.messageType.fullName

    val commandCases = service.commands
      .map { cmd =>
        val methodName = cmd.name
        val inputType = cmd.inputType.fullName
        s"""|case "$methodName":
            |  return entity().${lowerFirst(methodName)}(state, ($inputType) command);
            |""".stripMargin
      }

    s"""package $packageName;
        |
        |${writeImports(imports)}
        |
        |$managedComment
        |
        |/**
        | * A value entity handler that is the glue between the Protobuf service <code>${service.messageType.name}</code>
        | * and the command handler methods in the <code>${entity.messageType.name}</code> class.
        | */
        |public class ${className}Router extends ValueEntityRouter<$stateType, ${entity.messageType.name}> {
        |
        |  public ${className}Router(${entity.messageType.name} entity) {
        |    super(entity);
        |  }
        |
        |  @Override
        |  public ValueEntity.Effect<?> handleCommand(
        |      String commandName, $stateType state, Object command, CommandContext context) {
        |    switch (commandName) {
        |
        |      ${Format.indent(commandCases, 6)}
        |
        |      default:
        |        throw new ValueEntityRouter.CommandHandlerNotFound(commandName);
        |    }
        |  }
        |}
        |""".stripMargin

  }

  private[codegen] def valueEntityProvider(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.ValueEntity,
      packageName: String,
      className: String): String = {

    val relevantTypes = allRelevantMessageTypes(service, entity)
    val relevantProtoTypes = relevantTypes.collect { case proto: ProtoMessageType => proto }

    implicit val imports = generateImports(
      relevantTypes ++ relevantProtoTypes.flatMap(_.descriptorObject),
      packageName,
      otherImports = Seq(
        "kalix.javasdk.valueentity.ValueEntityContext",
        "kalix.javasdk.valueentity.ValueEntityOptions",
        "kalix.javasdk.valueentity.ValueEntityProvider",
        "com.google.protobuf.Descriptors",
        "java.util.function.Function"))

    val relevantTypeDescriptors =
      collectRelevantTypes(relevantProtoTypes, service.messageType)
        .flatMap(_.descriptorObject)
        .map { messageType => s"${messageType.name}.getDescriptor()" }

    val descriptors =
      (relevantTypeDescriptors :+ s"${service.messageType.parent.javaOuterClassname}.getDescriptor()").distinct.sorted

    s"""package $packageName;
        |
        |${writeImports(imports)}
        |
        |$managedComment
        |
        |/**
        | * A value entity provider that defines how to register and create the entity for
        | * the Protobuf service <code>${service.messageType.name}</code>.
        | *
        | * Should be used with the <code>register</code> method in {@link kalix.javasdk.Kalix}.
        | */
        |public class ${className}Provider implements ValueEntityProvider<${entity.state.messageType.fullName}, $className> {
        |
        |  private final Function<ValueEntityContext, $className> entityFactory;
        |  private final ValueEntityOptions options;
        |
        |  /** Factory method of ${className}Provider */
        |  public static ${className}Provider of(Function<ValueEntityContext, $className> entityFactory) {
        |    return new ${className}Provider(entityFactory, ValueEntityOptions.defaults());
        |  }
        |
        |  private ${className}Provider(
        |      Function<ValueEntityContext, $className> entityFactory,
        |      ValueEntityOptions options) {
        |    this.entityFactory = entityFactory;
        |    this.options = options;
        |  }
        |
        |  @Override
        |  public final ValueEntityOptions options() {
        |    return options;
        |  }
        |
        |  public final ${className}Provider withOptions(ValueEntityOptions options) {
        |    return new ${className}Provider(entityFactory, options);
        |  }
        |
        |  @Override
        |  public final Descriptors.ServiceDescriptor serviceDescriptor() {
        |    return ${typeName(service.messageType.descriptorImport)}.getDescriptor().findServiceByName("${service.messageType.name}");
        |  }
        |
        |  @Override
        |  public final String typeId() {
        |    return "${entity.typeId}";
        |  }
        |
        |  @Override
        |  public final ${className}Router newRouter(ValueEntityContext context) {
        |    return new ${className}Router(entityFactory.apply(context));
        |  }
        |
        |  @Override
        |  public final Descriptors.FileDescriptor[] additionalDescriptors() {
        |    return new Descriptors.FileDescriptor[] {
        |      ${Format.indent(descriptors.mkString(",\n"), 6)}
        |    };
        |  }
        |}
        |""".stripMargin

  }

  private[codegen] def abstractValueEntity(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.ValueEntity,
      packageName: String,
      className: String,
      mainPackageName: String): String = {

    val stateType = entity.state.messageType

    implicit val imports = generateImports(
      allRelevantMessageTypes(service, entity),
      packageName,
      otherImports = Seq(
        "kalix.javasdk.valueentity.ValueEntity",
        s"$mainPackageName.Components",
        s"$mainPackageName.ComponentsImpl"))

    val methods = service.commands
      .map { cmd =>
        val methodName = cmd.name

        s"""|public abstract Effect<${typeName(cmd.outputType)}> ${lowerFirst(methodName)}(${typeName(
          stateType)} currentState, ${typeName(cmd.inputType)} ${lowerFirst(cmd.inputType.name)});
            |""".stripMargin

      }

    s"""package $packageName;
        |
        |${writeImports(imports)}
        |
        |$managedComment
        |
        |public abstract class Abstract$className extends ValueEntity<${typeName(stateType)}> {
        |
        |  protected final Components components() {
        |    return new ComponentsImpl(commandContext());
        |  }
        |
        |  ${Format.indent(methods, 2)}
        |
        |}
        |""".stripMargin
  }
}
