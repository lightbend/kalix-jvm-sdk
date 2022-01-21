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

import com.lightbend.akkasls.codegen.Format
import com.lightbend.akkasls.codegen.Imports
import com.lightbend.akkasls.codegen.ModelBuilder
import com.lightbend.akkasls.codegen.SourceGeneratorUtils.allMessageTypes
import com.lightbend.akkasls.codegen.SourceGeneratorUtils.collectRelevantTypes
import com.lightbend.akkasls.codegen.SourceGeneratorUtils.generateImports
import com.lightbend.akkasls.codegen.SourceGeneratorUtils.generateSerializers
import com.lightbend.akkasls.codegen.SourceGeneratorUtils.lowerFirst
import com.lightbend.akkasls.codegen.SourceGeneratorUtils.managedComment
import com.lightbend.akkasls.codegen.SourceGeneratorUtils.qualifiedType
import com.lightbend.akkasls.codegen.SourceGeneratorUtils.unmanagedComment
import com.lightbend.akkasls.codegen.java.JavaGeneratorUtils.typeName
import com.lightbend.akkasls.codegen.java.JavaGeneratorUtils.writeImports

object EventSourcedEntitySourceGenerator {
  private[codegen] def eventSourcedEntityRouter(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.EventSourcedEntity,
      packageName: String,
      className: String): String = {

    val imports = generateImports(
      allMessageTypes(service, entity),
      packageName,
      otherImports = Seq(
        "com.akkaserverless.javasdk.eventsourcedentity.CommandContext",
        "com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity",
        "com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityRouter"))

    val stateType = entity.state.fqn.fullName

    val eventCases = {
      if (entity.events.isEmpty)
        List(s"throw new EventSourcedEntityRouter.EventHandlerNotFound(event.getClass());")
      else
        entity.events.zipWithIndex.map { case (evt, i) =>
          val eventType = evt.fqn.fullName
          s"""|${if (i == 0) "" else "} else "}if (event instanceof $eventType) {
              |  return entity().${lowerFirst(evt.fqn.name)}(state, ($eventType) event);""".stripMargin
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
       | * An event sourced entity handler that is the glue between the Protobuf service <code>${service.fqn.name}</code>
       | * and the command and event handler methods in the <code>${entity.fqn.name}</code> class.
       | */
       |public class ${className}Router extends EventSourcedEntityRouter<$stateType, ${entity.fqn.name}> {
       |
       |  public ${className}Router(${entity.fqn.name} entity) {
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

    val relevantTypes = allMessageTypes(service, entity)

    implicit val imports: Imports = generateImports(
      relevantTypes ++ relevantTypes.map(_.descriptorImport),
      packageName,
      otherImports = Seq(
        "com.akkaserverless.javasdk.impl.Serializer",
        "com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityContext",
        "com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityOptions",
        "com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityProvider",
        "com.google.protobuf.Descriptors",
        "java.util.function.Function"))

    val relevantDescriptors =
      collectRelevantTypes(relevantTypes, service.fqn)
        .collect { case fqn if fqn.isProtoMessage => s"${fqn.parent.javaOuterClassname}.getDescriptor()" }

    val descriptors =
      (relevantDescriptors :+ s"${service.fqn.parent.javaOuterClassname}.getDescriptor()").distinct.sorted

    val jsonSerializers = generateSerializers(relevantTypes.filterNot(_.isProtoMessage))

    s"""package $packageName;
       |
       |${writeImports(imports)}
       |
       |$managedComment
       |
       |/**
       | * An event sourced entity provider that defines how to register and create the entity for
       | * the Protobuf service <code>${service.fqn.name}</code>.
       | *
       | * Should be used with the <code>register</code> method in {@link com.akkaserverless.javasdk.AkkaServerless}.
       | */
       |public class ${className}Provider implements EventSourcedEntityProvider<${entity.state.fqn.fullName}, $className> {
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
       |    return ${typeName(service.fqn.descriptorImport)}.getDescriptor().findServiceByName("${service.fqn.name}");
       |  }
       |
       |  @Override
       |  public final String entityType() {
       |    return "${entity.entityType}";
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
       |  
       |  @Override
       |  public Serializer serializer() { 
       |    return ${Format.indent(jsonSerializers, 12)};
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
      allMessageTypes(service, entity),
      packageName,
      Seq(
        "com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityContext",
        "com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity",
        "com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity.Effect"))

    val commandHandlers =
      service.commands
        .map { command =>
          s"""|@Override
              |public Effect<${qualifiedType(command.outputType)}> ${lowerFirst(command.name)}(${qualifiedType(
            entity.state.fqn)} currentState, ${qualifiedType(command.inputType)} ${lowerFirst(command.inputType.name)}) {
              |  return effects().error("The command handler for `${command.name}` is not implemented, yet");
              |}
              |""".stripMargin
        }

    val eventHandlers =
      entity match {
        case ModelBuilder.EventSourcedEntity(_, _, _, events) =>
          events.map { event =>
            s"""|@Override
                |public ${qualifiedType(entity.state.fqn)} ${lowerFirst(event.fqn.name)}(${qualifiedType(
              entity.state.fqn)} currentState, ${qualifiedType(event.fqn)} ${lowerFirst(event.fqn.name)}) {
                |  throw new RuntimeException("The event handler for `${event.fqn.name}` is not implemented, yet");
                |}""".stripMargin
          }
      }

    s"""package $packageName;
       |
       |${writeImports(imports)}
       |
       |${unmanagedComment(Right(entity))}
       |
       |/** An event sourced entity. */
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
       |  public ${qualifiedType(entity.state.fqn)} emptyState() {
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
      allMessageTypes(service, entity),
      packageName,
      Seq(
        "com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity",
        s"$mainPackageName.Components",
        s"$mainPackageName.ComponentsImpl"))

    val commandHandlers = service.commands.map { command =>
      s"""|/** Command handler for "${command.name}". */
          |public abstract Effect<${qualifiedType(command.outputType)}> ${lowerFirst(command.name)}(${qualifiedType(
        entity.state.fqn)} currentState, ${qualifiedType(command.inputType)} ${lowerFirst(command.inputType.name)});
          |""".stripMargin
    }

    val eventHandlers = entity.events.map { event =>
      s"""|/** Event handler for "${event.fqn.name}". */
          |public abstract ${qualifiedType(entity.state.fqn)} ${lowerFirst(event.fqn.name)}(${qualifiedType(
        entity.state.fqn)} currentState, ${qualifiedType(event.fqn)} ${lowerFirst(event.fqn.name)});
          |""".stripMargin
    }

    s"""package $packageName;
       |
       |${writeImports(imports)}
       |
       |$managedComment
       |
       |/** An event sourced entity. */
       |public abstract class Abstract${className} extends EventSourcedEntity<${qualifiedType(entity.state.fqn)}> {
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
