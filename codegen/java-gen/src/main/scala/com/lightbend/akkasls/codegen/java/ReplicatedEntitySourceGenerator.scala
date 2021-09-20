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

import com.lightbend.akkasls.codegen.ModelBuilder
import com.lightbend.akkasls.codegen.Format

object ReplicatedEntitySourceGenerator {
  import SourceGenerator._
  import com.lightbend.akkasls.codegen.SourceGeneratorUtils._

  private[codegen] def replicatedEntitySource(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.ReplicatedEntity,
      packageName: String,
      className: String): String = {

    val imports = generateImports(
      service.commands,
      entity.data.typeArguments,
      packageName,
      otherImports = Seq(
        s"com.akkaserverless.javasdk.replicatedentity.${entity.data.name}",
        "com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext") ++ extraImports(entity.data))

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
      val methodName = cmd.fqn.name
      val inputType = cmd.inputType.fullName
      val outputType = cmd.outputType.fullName
      s"""|@Override
          |public Effect<$outputType> ${lowerFirst(methodName)}($parameterizedDataType currentData, $inputType command) {
          |  return effects().error("The command handler for `$methodName` is not implemented, yet");
          |}
          |""".stripMargin
    }

    s"""|$unmanagedComment
        |package $packageName;
        |
        |$imports
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

  private[codegen] def replicatedEntityHandler(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.ReplicatedEntity,
      packageName: String,
      className: String): String = {

    val imports = generateImports(
      service.commands,
      entity.data.typeArguments,
      packageName,
      otherImports = Seq(
        "com.akkaserverless.javasdk.impl.replicatedentity.ReplicatedEntityHandler",
        "com.akkaserverless.javasdk.replicatedentity.CommandContext",
        "com.akkaserverless.javasdk.replicatedentity.ReplicatedEntity",
        s"com.akkaserverless.javasdk.replicatedentity.${entity.data.name}") ++ extraImports(entity.data))

    val parameterizedDataType = entity.data.name + parameterizeDataType(entity.data)

    val commandCases = service.commands
      .map { cmd =>
        val methodName = cmd.fqn.name
        val inputType = cmd.inputType.fullName
        s"""|case "$methodName":
            |  return entity().${lowerFirst(methodName)}(data, ($inputType) command);
            |""".stripMargin
      }

    s"""|$managedComment
        |package $packageName;
        |
        |$imports
        |
        |/**
        | * A replicated entity handler that is the glue between the Protobuf service <code>${service.fqn.name}</code>
        | * and the command handler methods in the <code>${entity.fqn.name}</code> class.
        | */
        |public class ${className}Handler extends ReplicatedEntityHandler<$parameterizedDataType, ${entity.fqn.name}> {
        |
        |  public ${className}Handler(${entity.fqn.name} entity) {
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
        |        throw new ReplicatedEntityHandler.CommandHandlerNotFound(commandName);
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

    val imports = generateImports(
      relevantTypes,
      packageName,
      otherImports = Seq(
        s"com.akkaserverless.javasdk.replicatedentity.${entity.data.name}",
        "com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext",
        "com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityOptions",
        "com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityProvider",
        "com.google.protobuf.Descriptors",
        "java.util.function.Function") ++ relevantTypes.map(_.descriptorImport)
        ++ extraImports(entity.data) ++ extraTypeImports(entity.data.typeArguments))

    val parameterizedDataType = entity.data.name + parameterizeDataType(entity.data)

    val descriptors =
      (collectRelevantTypes(relevantTypes, service.fqn)
        .map(d =>
          s"${d.parent.javaOuterClassname}.getDescriptor()") :+ s"${service.fqn.parent.javaOuterClassname}.getDescriptor()").distinct.sorted

    s"""|$managedComment
        |package $packageName;
        |
        |$imports
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
        |    return ${service.fqn.parent.javaOuterClassname}.getDescriptor().findServiceByName("${service.fqn.name}");
        |  }
        |
        |  @Override
        |  public final String entityType() {
        |    return "${entity.entityType}";
        |  }
        |
        |  @Override
        |  public final ${className}Handler newHandler(ReplicatedEntityContext context) {
        |    return new ${className}Handler(entityFactory.apply(context));
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

    val typeArguments = parameterizeDataType(entity.data)
    val parameterizedDataType = entity.data.name + typeArguments
    val baseClass = s"Replicated${entity.data.shortName}Entity"

    val imports = generateImports(
      service.commands,
      entity.data.typeArguments,
      packageName,
      otherImports = Seq(
        s"com.akkaserverless.javasdk.replicatedentity.${entity.data.name}",
        s"com.akkaserverless.javasdk.replicatedentity.$baseClass") ++ extraImports(entity.data))

    val methods = service.commands
      .map { cmd =>
        val methodName = cmd.fqn.name
        val inputType = cmd.inputType.fullName
        val outputType = cmd.outputType.fullName

        s"""|/** Command handler for "${cmd.fqn.name}". */
            |public abstract Effect<$outputType> ${lowerFirst(
          methodName)}($parameterizedDataType currentData, $inputType ${lowerFirst(cmd.inputType.name)});
            |""".stripMargin

      }

    s"""|$managedComment
        |package $packageName;
        |
        |$imports
        |
        |/** A replicated entity. */
        |public abstract class Abstract$className extends $baseClass$typeArguments {
        |
        |  ${Format.indent(methods, 2)}
        |
        |}
        |""".stripMargin
  }

  private[codegen] def extraImports(replicatedData: ModelBuilder.ReplicatedData): Seq[String] = {
    replicatedData match {
      // special case ReplicatedMap as heterogeneous with ReplicatedData values
      case _: ModelBuilder.ReplicatedMap => Seq("com.akkaserverless.javasdk.replicatedentity.ReplicatedData")
      case _                             => Seq.empty
    }
  }

  private def dataType(typeArgument: ModelBuilder.TypeArgument): String = typeArgument match {
    case ModelBuilder.MessageTypeArgument(fqn) => fqn.fullName
    case ModelBuilder.ScalarTypeArgument(scalar) =>
      scalar match {
        case ModelBuilder.ScalarType.Int32 | ModelBuilder.ScalarType.UInt32 | ModelBuilder.ScalarType.SInt32 |
            ModelBuilder.ScalarType.Fixed32 | ModelBuilder.ScalarType.SFixed32 =>
          "Integer"
        case ModelBuilder.ScalarType.Int64 | ModelBuilder.ScalarType.UInt64 | ModelBuilder.ScalarType.SInt64 |
            ModelBuilder.ScalarType.Fixed64 | ModelBuilder.ScalarType.SFixed64 =>
          "Long"
        case ModelBuilder.ScalarType.Double => "Double"
        case ModelBuilder.ScalarType.Float  => "Float"
        case ModelBuilder.ScalarType.Bool   => "Boolean"
        case ModelBuilder.ScalarType.String => "String"
        case ModelBuilder.ScalarType.Bytes  => "ByteString"
        case _                              => "?"
      }
  }

  private def parameterizeDataType(replicatedData: ModelBuilder.ReplicatedData): String = {
    val typeArguments = replicatedData match {
      // special case ReplicatedMap as heterogeneous with ReplicatedData values
      case ModelBuilder.ReplicatedMap(key) => Seq(dataType(key), "ReplicatedData")
      case data                            => data.typeArguments.map(dataType)
    }
    parameterizeTypes(typeArguments)
  }

  private def parameterizeTypes(types: Iterable[String]): String = {
    if (types.nonEmpty) types.mkString("<", ", ", ">") else ""
  }
}
