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
import com.lightbend.akkasls.codegen.Syntax
import com.lightbend.akkasls.codegen.Syntax._
import org.bitbucket.inkytonik.kiama.output.PrettyPrinterTypes.Document

object ValueEntitySourceGenerator {
  import SourceGenerator._
  import EntityServiceSourceGenerator.generateImports

  private[codegen] def valueEntitySource(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.ValueEntity,
      packageName: String,
      className: String
  ): String = {

    val imports = generateImports(
      service.commands,
      Some(entity.state),
      packageName,
      otherImports = Seq("com.akkaserverless.javasdk.valueentity.ValueEntityContext")
    )

    val serviceApiOuterClass = service.fqn.parent.javaOuterClassname
    val outerClassAndState = s"${entity.fqn.parent.javaOuterClassname}.${entity.state.fqn.name}"

    val methods = service.commands.map { cmd =>
      val methodName = cmd.fqn.name
      val inputType = s"$serviceApiOuterClass.${cmd.inputType.name}"
      val outputType = qualifiedType(cmd.outputType)

      s"""|@Override
          |public Effect<$outputType> ${lowerFirst(methodName)}($outerClassAndState currentState, $inputType command) {
          |  return effects().error("The command handler for `$methodName` is not implemented, yet");
          |}
          |""".stripMargin
    }

    s"""|$generatedCodeCommentString
        |package $packageName;
        |
        |$imports
        |
        |/** A value entity. */
        |public class $className extends Abstract${className} {
        |  @SuppressWarnings("unused")
        |  private final String entityId;
        |
        |  public ${className}(ValueEntityContext context) {
        |    this.entityId = context.entityId();
        |  }
        |
        |  @Override
        |  public $outerClassAndState emptyState() {
        |    throw new UnsupportedOperationException("Not implemented yet, replace with your empty entity state");
        |  }
        |
        |  ${Syntax.indent(methods, num = 2)}
        |}""".stripMargin
  }

  private[codegen] def valueEntityHandler(service: ModelBuilder.EntityService,
                                          entity: ModelBuilder.ValueEntity,
                                          packageName: String,
                                          className: String): String = {

    val imports = generateImports(
      service.commands,
      Some(entity.state),
      packageName,
      otherImports = Seq(
        "com.akkaserverless.javasdk.valueentity.CommandContext",
        "com.akkaserverless.javasdk.valueentity.ValueEntity",
        "com.akkaserverless.javasdk.impl.valueentity.ValueEntityHandler"
      )
    )

    val serviceApiOuterClass = service.fqn.parent.javaOuterClassname
    val outerClassAndState = s"${entity.fqn.parent.javaOuterClassname}.${entity.state.fqn.name}"

    val commandCases = service.commands
      .map { cmd =>
        val methodName = cmd.fqn.name
        val inputType = s"$serviceApiOuterClass.${cmd.inputType.name}"
        s"""|case "$methodName":
            |  return entity().${lowerFirst(methodName)}(state, ($inputType) command);
            |""".stripMargin
      }

    s"""|$managedCodeCommentString
        |package $packageName;
        |
        |$imports
        |
        |/**
        | * A value entity handler that is the glue between the Protobuf service <code>${service.fqn.name}</code>
        | * and the command handler methods in the <code>${entity.fqn.name}</code> class.
        | */
        |public class ${className}Handler extends ValueEntityHandler<$outerClassAndState, ${entity.fqn.name}> {
        |
        |  public ${className}Handler(${entity.fqn.name} entity) {
        |    super(entity);
        |  }
        |
        |  @Override
        |  public ValueEntity.Effect<?> handleCommand(
        |      String commandName, $outerClassAndState state, Object command, CommandContext context) {
        |    switch (commandName) {
        |
        |      ${Syntax.indent(commandCases, 6)}
        |
        |      default:
        |        throw new ValueEntityHandler.CommandHandlerNotFound(commandName);
        |    }
        |  }
        |}""".stripMargin

  }

  private[codegen] def valueEntityProvider(service: ModelBuilder.EntityService,
                                           entity: ModelBuilder.ValueEntity,
                                           packageName: String,
                                           className: String): String = {
    val relevantTypes = {
      List(entity.state.fqn) ++
      service.commands.flatMap { cmd =>
        cmd.inputType :: cmd.outputType :: Nil
      }
    }

    val imports = generateImports(
      relevantTypes,
      packageName,
      otherImports = Seq(
          "com.akkaserverless.javasdk.valueentity.ValueEntityContext",
          "com.akkaserverless.javasdk.valueentity.ValueEntityOptions",
          "com.akkaserverless.javasdk.valueentity.ValueEntityProvider",
          "com.google.protobuf.Descriptors",
          "java.util.function.Function"
        ) ++ relevantTypes.map(fqn => fqn.parent.javaPackage + "." + fqn.parent.javaOuterClassname)
    )

    val descriptors =
      (collectRelevantTypes(relevantTypes, service.fqn)
        .map(d => s"${d.parent.javaOuterClassname}.getDescriptor()") :+ s"${service.fqn.parent.javaOuterClassname}.getDescriptor()").distinct.sorted

    s"""|$managedCodeCommentString
        |package $packageName;
        |
        |$imports
        |
        |/**
        | * A value entity provider that defines how to register and create the entity for
        | * the Protobuf service <code>${service.fqn.name}</code>.
        | *
        | * Should be used with the <code>register</code> method in {@link com.akkaserverless.javasdk.AkkaServerless}.
        | */
        |public class ${className}Provider implements ValueEntityProvider<${entity.fqn.parent.javaOuterClassname}.${entity.state.fqn.name}, ${className}> {
        |
        |  private final Function<ValueEntityContext, ${className}> entityFactory;
        |  private final ValueEntityOptions options;
        |
        |  /** Factory method of ${className}Provider */
        |  public static ${className}Provider of(Function<ValueEntityContext, ${className}> entityFactory) {
        |    return new ${className}Provider(entityFactory, ValueEntityOptions.defaults());
        |  }
        | 
        |  private ${className}Provider(
        |      Function<ValueEntityContext, ${className}> entityFactory,
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
        |    return ${service.fqn.parent.javaOuterClassname}.getDescriptor().findServiceByName("${service.fqn.name}");
        |  }
        |
        |  @Override
        |  public final String entityType() {
        |    return "${entity.entityType}";
        |  }
        |
        |  @Override
        |  public final ${className}Handler newHandler(ValueEntityContext context) {
        |    return new ${className}Handler(entityFactory.apply(context));
        |  }
        |
        |  @Override
        |  public final Descriptors.FileDescriptor[] additionalDescriptors() {
        |    return new Descriptors.FileDescriptor[] {
        |      ${Syntax.indent(descriptors.mkString(",\n"), 6)}
        |    };
        |  }
        |}""".stripMargin

  }

  private[codegen] def abstractValueEntity(
      service: ModelBuilder.EntityService,
      entity: ModelBuilder.ValueEntity,
      packageName: String,
      className: String
  ): String = {

    val serviceApiOuterClass = service.fqn.parent.javaOuterClassname
    val outerClassAndState = s"${entity.fqn.parent.javaOuterClassname}.${entity.state.fqn.name}"

    val imports = generateImports(
      service.commands,
      Some(entity.state),
      packageName,
      otherImports = Seq(
        "com.akkaserverless.javasdk.valueentity.ValueEntity"
      )
    )

    val methods = service.commands
      .map { cmd =>
        val methodName = cmd.fqn.name

        val inputType = s"$serviceApiOuterClass.${cmd.inputType.name}"
        val outputType = qualifiedType(cmd.outputType)

        s"""|/** Command handler for "${cmd.fqn.name}". */
            |public abstract Effect<$outputType> ${lowerFirst(methodName)}($outerClassAndState currentState, $inputType ${lowerFirst(
             cmd.inputType.name
           )});
            |""".stripMargin

      }

    s"""|$managedCodeCommentString
        |package $packageName;
        |
        |$imports
        |
        |/** A value entity. */
        |public abstract class Abstract${className} extends ValueEntity<$outerClassAndState> {
        |
        |  ${Syntax.indent(methods, 2)}
        |
        |}""".stripMargin
  }
}
