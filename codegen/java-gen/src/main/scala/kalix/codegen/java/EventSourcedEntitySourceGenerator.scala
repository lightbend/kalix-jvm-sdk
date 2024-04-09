/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.codegen.java

import kalix.codegen.Format
import kalix.codegen.Imports
import kalix.codegen.ModelBuilder
import kalix.codegen.ProtoMessageType
import kalix.codegen.SourceGeneratorUtils.allRelevantMessageTypes
import kalix.codegen.SourceGeneratorUtils.collectRelevantTypes
import kalix.codegen.SourceGeneratorUtils.generateImports
import kalix.codegen.SourceGeneratorUtils.lowerFirst
import kalix.codegen.SourceGeneratorUtils.managedComment
import kalix.codegen.SourceGeneratorUtils.qualifiedType
import kalix.codegen.SourceGeneratorUtils.unmanagedComment
import kalix.codegen.java.JavaGeneratorUtils.typeName
import kalix.codegen.java.JavaGeneratorUtils.writeImports

object EventSourcedEntitySourceGenerator {
  private[codegen] def eventSourcedEntityRouter(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.EventSourcedEntity,
      packageName: String,
      className: String): String = {

    val imports = generateImports(
      allRelevantMessageTypes(service, entity),
      packageName,
      otherImports = Seq(
        "kalix.javasdk.eventsourcedentity.CommandContext",
        "kalix.javasdk.eventsourcedentity.EventSourcedEntity",
        "kalix.javasdk.impl.eventsourcedentity.EventSourcedEntityRouter"))

    val stateType = entity.state.messageType.fullName

    val eventCases = {
      if (entity.events.isEmpty)
        List(s"throw new EventSourcedEntityRouter.EventHandlerNotFound(event.getClass());")
      else
        entity.events.zipWithIndex.map { case (evt, i) =>
          val eventType = evt.messageType.fullName
          s"""|${if (i == 0) "" else "} else "}if (event instanceof $eventType) {
              |  return entity().${lowerFirst(evt.messageType.name)}(state, ($eventType) event);""".stripMargin
        }.toSeq :+
        s"""|} else {
              |  throw new EventSourcedEntityRouter.EventHandlerNotFound(event.getClass());
              |}""".stripMargin
    }

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
       | * An event sourced entity handler that is the glue between the Protobuf service <code>${service.messageType.name}</code>
       | * and the command and event handler methods in the <code>${entity.messageType.name}</code> class.
       | */
       |public class ${className}Router extends EventSourcedEntityRouter<$stateType, Object, ${entity.messageType.name}> {
       |
       |  public ${className}Router(${entity.messageType.name} entity) {
       |    super(entity);
       |  }
       |
       |  @Override
       |  public $stateType handleEvent($stateType state, Object event) {
       |    ${Format.indent(eventCases, 4)}
       |  }
       |
       |  @Override
       |  public EventSourcedEntity.Effect<?> handleCommand(
       |      String commandName, $stateType state, Object command, CommandContext context) {
       |    switch (commandName) {
       |
       |      ${Format.indent(commandCases, 6)}
       |
       |      default:
       |        throw new EventSourcedEntityRouter.CommandHandlerNotFound(commandName);
       |    }
       |  }
       |}
       |""".stripMargin

  }

  private[codegen] def eventSourcedEntityProvider(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.EventSourcedEntity,
      packageName: String,
      className: String): String = {

    val relevantTypes = allRelevantMessageTypes(service, entity)
    val relevantProtoTypes = relevantTypes.collect { case proto: ProtoMessageType => proto }

    implicit val imports: Imports = generateImports(
      relevantTypes ++ relevantProtoTypes.map(_.descriptorImport),
      packageName,
      otherImports = Seq(
        "kalix.javasdk.eventsourcedentity.EventSourcedEntityContext",
        "kalix.javasdk.eventsourcedentity.EventSourcedEntityOptions",
        "kalix.javasdk.eventsourcedentity.EventSourcedEntityProvider",
        "com.google.protobuf.Descriptors",
        "java.util.function.Function"))

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
       | * An event sourced entity provider that defines how to register and create the entity for
       | * the Protobuf service <code>${service.messageType.name}</code>.
       | *
       | * Should be used with the <code>register</code> method in {@link kalix.javasdk.Kalix}.
       | */
       |public class ${className}Provider implements EventSourcedEntityProvider<${entity.state.messageType.fullName}, Object, $className> {
       |
       |  private final Function<EventSourcedEntityContext, $className> entityFactory;
       |  private final EventSourcedEntityOptions options;
       |
       |  /** Factory method of ${className}Provider */
       |  public static ${className}Provider of(Function<EventSourcedEntityContext, $className> entityFactory) {
       |    return new ${className}Provider(entityFactory, EventSourcedEntityOptions.defaults());
       |  }
       |
       |  private ${className}Provider(
       |      Function<EventSourcedEntityContext, $className> entityFactory,
       |      EventSourcedEntityOptions options) {
       |    this.entityFactory = entityFactory;
       |    this.options = options;
       |  }
       |
       |  @Override
       |  public final EventSourcedEntityOptions options() {
       |    return options;
       |  }
       |
       |  public final ${className}Provider withOptions(EventSourcedEntityOptions options) {
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
       |  public final ${className}Router newRouter(EventSourcedEntityContext context) {
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

  private[codegen] def eventSourcedEntitySource(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.EventSourcedEntity,
      packageName: String,
      className: String,
      interfaceClassName: String): String = {

    val imports = generateImports(
      allRelevantMessageTypes(service, entity),
      packageName,
      Seq(
        "kalix.javasdk.eventsourcedentity.EventSourcedEntityContext",
        "kalix.javasdk.eventsourcedentity.EventSourcedEntity",
        "kalix.javasdk.eventsourcedentity.EventSourcedEntity.Effect"))

    val commandHandlers =
      service.commands
        .map { command =>
          s"""|@Override
              |public Effect<${qualifiedType(command.outputType)}> ${lowerFirst(command.name)}(${qualifiedType(
            entity.state.messageType)} currentState, ${qualifiedType(command.inputType)} ${lowerFirst(
            command.inputType.name)}) {
              |  return effects().error("The command handler for `${command.name}` is not implemented, yet");
              |}
              |""".stripMargin
        }

    val eventHandlers =
      entity match {
        case ModelBuilder.EventSourcedEntity(_, _, _, events) =>
          events.map { event =>
            s"""|@Override
                |public ${qualifiedType(entity.state.messageType)} ${lowerFirst(
              event.messageType.name)}(${qualifiedType(entity.state.messageType)} currentState, ${qualifiedType(
              event.messageType)} ${lowerFirst(event.messageType.name)}) {
                |  throw new RuntimeException("The event handler for `${event.messageType.name}` is not implemented, yet");
                |}""".stripMargin
          }
      }

    s"""package $packageName;
       |
       |${writeImports(imports)}
       |
       |${unmanagedComment(Right(entity))}
       |
       |public class $className extends ${interfaceClassName} {
       |
       |  @SuppressWarnings("unused")
       |  private final String entityId;
       |
       |  public $className(EventSourcedEntityContext context) {
       |    this.entityId = context.entityId();
       |  }
       |
       |  @Override
       |  public ${qualifiedType(entity.state.messageType)} emptyState() {
       |    throw new UnsupportedOperationException("Not implemented yet, replace with your empty entity state");
       |  }
       |
       |  ${Format.indent(commandHandlers, num = 2)}
       |
       |  ${Format.indent(eventHandlers, num = 2)}
       |
       |}
       |""".stripMargin
  }

  private[codegen] def abstractEventSourcedEntity(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.EventSourcedEntity,
      packageName: String,
      className: String,
      mainPackageName: String): String = {

    val imports = generateImports(
      allRelevantMessageTypes(service, entity),
      packageName,
      Seq(
        "kalix.javasdk.eventsourcedentity.EventSourcedEntity",
        s"$mainPackageName.Components",
        s"$mainPackageName.ComponentsImpl"))

    val commandHandlers = service.commands.map { command =>
      s"""|public abstract Effect<${qualifiedType(command.outputType)}> ${lowerFirst(command.name)}(${qualifiedType(
        entity.state.messageType)} currentState, ${qualifiedType(command.inputType)} ${lowerFirst(
        command.inputType.name)});
          |""".stripMargin
    }

    val eventHandlers = entity.events.map { event =>
      s"""|public abstract ${qualifiedType(entity.state.messageType)} ${lowerFirst(
        event.messageType.name)}(${qualifiedType(entity.state.messageType)} currentState, ${qualifiedType(
        event.messageType)} ${lowerFirst(event.messageType.name)});
          |""".stripMargin
    }

    s"""package $packageName;
       |
       |${writeImports(imports)}
       |
       |$managedComment
       |
       |public abstract class Abstract${className} extends EventSourcedEntity<${qualifiedType(entity.state.messageType)}, Object> {
       |
       |  protected final Components components() {
       |    return new ComponentsImpl(commandContext());
       |  }
       |
       |  ${Format.indent(commandHandlers, num = 2)}
       |
       |  ${Format.indent(eventHandlers, num = 2)}
       |
       |}""".stripMargin
  }
}
