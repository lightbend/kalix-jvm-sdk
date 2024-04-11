/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.codegen.java

import kalix.codegen.Format
import kalix.codegen.ModelBuilder
import kalix.codegen.ProtoMessageType

object ReplicatedEntitySourceGenerator {
  import JavaGeneratorUtils._
  import kalix.codegen.SourceGeneratorUtils._

  private[codegen] def replicatedEntitySource(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.ReplicatedEntity,
      packageName: String,
      className: String): String = {

    implicit val imports = generateCommandAndTypeArgumentImports(
      service.commands,
      entity.data.typeArguments,
      packageName,
      otherImports = Seq(
        s"kalix.javasdk.replicatedentity.${entity.data.name}",
        "kalix.javasdk.replicatedentity.ReplicatedEntityContext") ++ extraReplicatedImports(entity.data))

    val parameterizedDataType = entity.data.name + parameterizeDataType(entity.data)

    val emptyValue = entity.data match {
      case ModelBuilder.ReplicatedRegister(valueType) =>
        s"""|
            |  @Override
            |  public ${dataType(valueType)} emptyValue() {
            |    throw new UnsupportedOperationException("Not implemented yet, replace with your empty register value");
            |  }
            |""".stripMargin
      case _ => ""
    }

    val methods = service.commands.map { cmd =>
      val methodName = cmd.name
      val inputType = cmd.inputType.fullName
      val outputType = cmd.outputType.fullName
      s"""|@Override
          |public Effect<$outputType> ${lowerFirst(
        methodName)}($parameterizedDataType currentData, $inputType ${lowerFirst(cmd.inputType.name)}) {
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
        |  public $className(ReplicatedEntityContext context) {
        |    this.entityId = context.entityId();
        |  }
        |$emptyValue
        |  ${Format.indent(methods, num = 2)}
        |}
        |""".stripMargin
  }

  private[codegen] def replicatedEntityRouter(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.ReplicatedEntity,
      packageName: String,
      className: String): String = {

    implicit val imports = generateCommandAndTypeArgumentImports(
      service.commands,
      entity.data.typeArguments,
      packageName,
      otherImports = Seq(
        "kalix.javasdk.impl.replicatedentity.ReplicatedEntityRouter",
        "kalix.javasdk.replicatedentity.CommandContext",
        "kalix.javasdk.replicatedentity.ReplicatedEntity",
        s"kalix.javasdk.replicatedentity.${entity.data.name}") ++ extraReplicatedImports(entity.data))

    val parameterizedDataType = entity.data.name + parameterizeDataType(entity.data)

    val commandCases = service.commands
      .map { cmd =>
        val methodName = cmd.name
        val inputType = cmd.inputType.fullName
        s"""|case "$methodName":
            |  return entity().${lowerFirst(methodName)}(data, ($inputType) command);
            |""".stripMargin
      }

    s"""package $packageName;
        |
        |${writeImports(imports)}
        |
        |$managedComment
        |
        |/**
        | * A replicated entity handler that is the glue between the Protobuf service <code>${service.messageType.name}</code>
        | * and the command handler methods in the <code>${entity.messageType.name}</code> class.
        | */
        |public class ${className}Router extends ReplicatedEntityRouter<$parameterizedDataType, ${entity.messageType.name}> {
        |
        |  public ${className}Router(${entity.messageType.name} entity) {
        |    super(entity);
        |  }
        |
        |  @Override
        |  public ReplicatedEntity.Effect<?> handleCommand(
        |      String commandName, $parameterizedDataType data, Object command, CommandContext context) {
        |    switch (commandName) {
        |
        |      ${Format.indent(commandCases, 6)}
        |
        |      default:
        |        throw new ReplicatedEntityRouter.CommandHandlerNotFound(commandName);
        |    }
        |  }
        |}
        |""".stripMargin

  }

  private[codegen] def replicatedEntityProvider(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.ReplicatedEntity,
      packageName: String,
      className: String): String = {

    val relevantTypes = allRelevantMessageTypes(service, entity)
    val relevantProtoTypes = relevantTypes.collect { case proto: ProtoMessageType => proto }

    implicit val imports = generateImports(
      relevantTypes ++ relevantProtoTypes.map(_.descriptorImport),
      packageName,
      otherImports = Seq(
        s"kalix.javasdk.replicatedentity.${entity.data.name}",
        "kalix.javasdk.replicatedentity.ReplicatedEntityContext",
        "kalix.javasdk.replicatedentity.ReplicatedEntityOptions",
        "kalix.javasdk.replicatedentity.ReplicatedEntityProvider",
        "com.google.protobuf.Descriptors",
        "java.util.function.Function")
        ++ extraReplicatedImports(entity.data) ++ extraTypeImports(entity.data.typeArguments))

    val parameterizedDataType = entity.data.name + parameterizeDataType(entity.data)

    val relevantDescriptors =
      collectRelevantTypes(relevantProtoTypes, service.messageType)
        .map { messageType => s"${messageType.parent.javaOuterClassname}.getDescriptor()" }

    val descriptors =
      (relevantDescriptors :+ s"${service.messageType.parent.javaOuterClassname}.getDescriptor()").distinct.sorted

    s"""package $packageName;
        |
        |${writeImports(imports)}
        |
        |$managedComment
        |
        |/**
        | * A replicated entity provider that defines how to register and create the entity for
        | * the Protobuf service <code>${service.messageType.name}</code>.
        | *
        | * Should be used with the <code>register</code> method in {@link kalix.javasdk.Kalix}.
        | */
        |public class ${className}Provider implements ReplicatedEntityProvider<$parameterizedDataType, $className> {
        |
        |  private final Function<ReplicatedEntityContext, $className> entityFactory;
        |  private final ReplicatedEntityOptions options;
        |
        |  /** Factory method of ${className}Provider */
        |  public static ${className}Provider of(Function<ReplicatedEntityContext, $className> entityFactory) {
        |    return new ${className}Provider(entityFactory, ReplicatedEntityOptions.defaults());
        |  }
        |
        |  private ${className}Provider(
        |      Function<ReplicatedEntityContext, $className> entityFactory,
        |      ReplicatedEntityOptions options) {
        |    this.entityFactory = entityFactory;
        |    this.options = options;
        |  }
        |
        |  @Override
        |  public final ReplicatedEntityOptions options() {
        |    return options;
        |  }
        |
        |  public final ${className}Provider withOptions(ReplicatedEntityOptions options) {
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
        |  public final ${className}Router newRouter(ReplicatedEntityContext context) {
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

  private[codegen] def abstractReplicatedEntity(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.ReplicatedEntity,
      packageName: String,
      mainPackageName: String): String = {

    val abstractEntityName = entity.abstractEntityName
    val baseClass = entity.data.baseClass

    implicit val imports = generateCommandAndTypeArgumentImports(
      service.commands,
      entity.data.typeArguments,
      packageName,
      otherImports = Seq(
        s"kalix.javasdk.replicatedentity.${entity.data.name}",
        s"kalix.javasdk.replicatedentity.$baseClass",
        s"$mainPackageName.Components",
        s"$mainPackageName.ComponentsImpl") ++ extraReplicatedImports(entity.data))

    val typeArguments = parameterizeDataType(entity.data)
    val parameterizedDataType = entity.data.name + typeArguments

    val methods = service.commands
      .map { cmd =>
        val methodName = cmd.name
        val inputType = cmd.inputType.fullName
        val outputType = cmd.outputType.fullName

        s"""|public abstract Effect<$outputType> ${lowerFirst(
          methodName)}($parameterizedDataType currentData, $inputType ${lowerFirst(cmd.inputType.name)});
            |""".stripMargin
      }

    s"""package $packageName;
        |
        |${writeImports(imports)}
        |
        |$managedComment
        |
        |public abstract class $abstractEntityName extends $baseClass$typeArguments {
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
