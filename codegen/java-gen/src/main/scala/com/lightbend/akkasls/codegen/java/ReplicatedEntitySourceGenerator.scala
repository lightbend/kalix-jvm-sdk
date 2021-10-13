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
import com.lightbend.akkasls.codegen.ModelBuilder

object ReplicatedEntitySourceGenerator {
  import com.lightbend.akkasls.codegen.SourceGeneratorUtils._
  import JavaGeneratorUtils._

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
        s"com.akkaserverless.javasdk.replicatedentity.${entity.data.name}",
        "com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext") ++ extraReplicatedImports(entity.data))

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
        |$unmanagedComment
        |
        |/** A replicated entity. */
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
        "com.akkaserverless.javasdk.impl.replicatedentity.ReplicatedEntityRouter",
        "com.akkaserverless.javasdk.replicatedentity.CommandContext",
        "com.akkaserverless.javasdk.replicatedentity.ReplicatedEntity",
        s"com.akkaserverless.javasdk.replicatedentity.${entity.data.name}") ++ extraReplicatedImports(entity.data))

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
        | * A replicated entity handler that is the glue between the Protobuf service <code>${service.fqn.name}</code>
        | * and the command handler methods in the <code>${entity.fqn.name}</code> class.
        | */
        |public class ${className}Router extends ReplicatedEntityRouter<$parameterizedDataType, ${entity.fqn.name}> {
        |
        |  public ${className}Router(${entity.fqn.name} entity) {
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

    val relevantTypes = {
      entity.data.typeArguments.collect { case ModelBuilder.MessageTypeArgument(fqn) =>
        fqn
      } ++ service.commands.flatMap { cmd =>
        cmd.inputType :: cmd.outputType :: Nil
      }
    }

    implicit val imports = generateImports(
      relevantTypes ++ relevantTypes.map(_.descriptorImport),
      packageName,
      otherImports = Seq(
        s"com.akkaserverless.javasdk.replicatedentity.${entity.data.name}",
        "com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext",
        "com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityOptions",
        "com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityProvider",
        "com.google.protobuf.Descriptors",
        "java.util.function.Function")
        ++ extraReplicatedImports(entity.data) ++ extraTypeImports(entity.data.typeArguments))

    val parameterizedDataType = entity.data.name + parameterizeDataType(entity.data)

    val descriptors =
      (collectRelevantTypes(relevantTypes, service.fqn)
        .map(d =>
          s"${d.parent.javaOuterClassname}.getDescriptor()") :+ s"${service.fqn.parent.javaOuterClassname}.getDescriptor()").distinct.sorted

    s"""package $packageName;
        |
        |${writeImports(imports)}
        |
        |$managedComment
        |
        |/**
        | * A replicated entity provider that defines how to register and create the entity for
        | * the Protobuf service <code>${service.fqn.name}</code>.
        | *
        | * Should be used with the <code>register</code> method in {@link com.akkaserverless.javasdk.AkkaServerless}.
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
        |    return ${typeName(service.fqn.descriptorImport)}.getDescriptor().findServiceByName("${service.fqn.name}");
        |  }
        |
        |  @Override
        |  public final String entityType() {
        |    return "${entity.entityType}";
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
      className: String): String = {

    val abstractEntityName = entity.abstractEntityName
    val baseClass = entity.data.baseClass

    implicit val imports = generateCommandAndTypeArgumentImports(
      service.commands,
      entity.data.typeArguments,
      packageName,
      otherImports = Seq(
        s"com.akkaserverless.javasdk.replicatedentity.${entity.data.name}",
        s"com.akkaserverless.javasdk.replicatedentity.$baseClass") ++ extraReplicatedImports(entity.data))

    val typeArguments = parameterizeDataType(entity.data)
    val parameterizedDataType = entity.data.name + typeArguments

    val methods = service.commands
      .map { cmd =>
        val methodName = cmd.name
        val inputType = cmd.inputType.fullName
        val outputType = cmd.outputType.fullName

        s"""|/** Command handler for "${cmd.name}". */
            |public abstract Effect<$outputType> ${lowerFirst(
          methodName)}($parameterizedDataType currentData, $inputType ${lowerFirst(cmd.inputType.name)});
            |""".stripMargin
      }

    s"""package $packageName;
        |
        |${writeImports(imports)}
        |
        |$managedComment
        |
        |/** A replicated entity. */
        |public abstract class $abstractEntityName extends $baseClass$typeArguments {
        |
        |  ${Format.indent(methods, 2)}
        |
        |}
        |""".stripMargin
  }
}
