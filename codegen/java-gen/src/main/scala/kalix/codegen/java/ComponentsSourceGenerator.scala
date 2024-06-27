/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.codegen.java

import kalix.codegen.File
import kalix.codegen.Format
import kalix.codegen.ProtoMessageType
import kalix.codegen.GeneratedFiles
import kalix.codegen.ModelBuilder
import kalix.codegen.ModelBuilder.ActionService
import kalix.codegen.ModelBuilder.EntityService
import kalix.codegen.ModelBuilder.Service
import kalix.codegen.ModelBuilder.ViewService
import kalix.codegen.PackageNaming

/**
 * Generates convenience accessors for other components in the same service, accessible from actions
 */
object ComponentsSourceGenerator {
  import JavaGeneratorUtils._
  import kalix.codegen.SourceGeneratorUtils._

  private final case class CallableComponent(
      uniqueName: String,
      service: ModelBuilder.Service,
      callableCommands: Iterable[ModelBuilder.Command])

  def generate(pkg: PackageNaming, serviceMap: Map[String, Service]): GeneratedFiles = {

    // since we want to flatten component names to as short as possible there may be duplicate
    // names, so for those we need to use a longer name
    val services = serviceMap.values.toSeq.sortBy(_.messageType.name)
    val uniqueNamesAndComponents: Seq[CallableComponent] = services
      .flatMap { component =>
        val callableCommands = callableCommandsFor(component)
        if (callableCommands.nonEmpty) {
          val name = nameFor(component)
          if (services.exists(other => other != component && nameFor(other) == name)) {
            // conflict give it a longer unique name
            // FIXME component.messageType.name is the gRPC service name, not the component class which is what we want
            val uniqueName =
              component.messageType.parent.javaPackage.replaceAllLiterally(".", "_") + "_" + component.messageType.name
            Some(CallableComponent(uniqueName, component, callableCommands))
          } else {
            Some(CallableComponent(name, component, callableCommands))
          }
        } else {
          None
        }
      }

    GeneratedFiles.Empty
      .addManaged(File.java(pkg, "Components", generateComponentsInterface(pkg.javaPackage, uniqueNamesAndComponents)))
      .addManaged(File.java(pkg, "ComponentsImpl", generateComponentsImpl(pkg.javaPackage, uniqueNamesAndComponents)))
  }

  private def generateComponentsInterface(packageName: String, callableComponents: Seq[CallableComponent]): String = {
    val imports = generateImports(Nil, packageName, otherImports = Seq("kalix.javasdk.DeferredCall"))

    val componentCalls = callableComponents.map { component =>
      // higher risk of name conflicts here where all components in the service meets up, so all
      // type names are fully qualified rather than imported
      val methods = component.callableCommands
        .map { command =>
          val inputType = fullyQualifiedMessage(command.inputType)
          val outputType = fullyQualifiedMessage(command.outputType)
          s"""DeferredCall<$inputType, $outputType> ${lowerFirst(command.name)}($inputType ${lowerFirst(
            command.inputType.name)});
             |""".stripMargin
        }

      s"""interface ${component.uniqueName}Calls {
         |  ${Format.indent(methods, 2)}
         |}""".stripMargin
    }

    val componentGetters = callableComponents.map { component =>
      s"${component.uniqueName}Calls ${lowerFirst(component.uniqueName)}();"
    }

    s"""package $packageName;
      |
      |${writeImports(imports)}
      |
      |$managedComment
      |
      |/**
      | * Not intended for user extension, provided through generated implementation
      | */
      |public interface Components {
      |  ${Format.indent(componentGetters, 2)}
      |
      |  ${Format.indent(componentCalls, 2)}
      |}
      |""".stripMargin
  }

  private def generateComponentsImpl(packageName: String, components: Seq[CallableComponent]): String = {
    val imports = generateImports(
      Nil,
      packageName,
      otherImports = Seq(
        "akka.grpc.javadsl.SingleResponseRequestBuilder",
        "kalix.javasdk.DeferredCall",
        "kalix.javasdk.Context",
        "kalix.javasdk.Metadata",
        "kalix.javasdk.impl.GrpcDeferredCall",
        "kalix.javasdk.impl.MetadataImpl",
        "kalix.javasdk.impl.InternalContext"))

    val componentGetters = components.map { component =>
      s"""@Override
         |public Components.${component.uniqueName}Calls ${lowerFirst(component.uniqueName)}() {
         |  return new ${component.uniqueName}CallsImpl();
         |}""".stripMargin
    }

    val componentCallImpls = components.map { component =>
      val methods = component.callableCommands
        .map { command =>
          val commandMethod = lowerFirst(command.name)
          val paramName = lowerFirst(command.inputType.name)
          val inputType = fullyQualifiedMessage(command.inputType)
          val outputType = fullyQualifiedMessage(command.outputType)
          val messageType = component.service.messageType
          s"""@Override
             |public DeferredCall<$inputType, $outputType> $commandMethod($inputType $paramName) {
             |  return new GrpcDeferredCall<>(
             |    ${lowerFirst(command.inputType.name)},
             |    context.componentCallMetadata(),
             |    "${component.service.messageType.fullyQualifiedProtoName}",
             |    "${command.name}",
             |    (Metadata metadata) -> {
             |      ${messageType.fullyQualifiedGrpcServiceInterfaceName} client = getGrpcClient(${messageType.fullyQualifiedGrpcServiceInterfaceName}.class);
             |      if (client instanceof ${messageType.fullyQualifiedGrpcServiceInterfaceName}Client) {
             |        return addHeaders(((${messageType.fullyQualifiedGrpcServiceInterfaceName}Client) client).$commandMethod(), metadata).invoke($paramName);
             |      } else {
             |        // only for tests with mocked client implementation
             |        return client.$commandMethod($paramName);
             |      }
             |    }
             |  );
             |}""".stripMargin
        }

      s"""private final class ${component.uniqueName}CallsImpl implements Components.${component.uniqueName}Calls {
         |   ${Format.indent(methods, 2)}
         |}""".stripMargin

    }

    s"""package $packageName;
       |
       |${writeImports(imports)}
       |
       |$managedComment
       |
       |/**
       | * Not intended for direct instantiation, called by generated code, use Action.components() to access
       | */
       |public final class ComponentsImpl implements Components {
       |
       |  private final InternalContext context;
       |
       |  public ComponentsImpl(Context context) {
       |    this.context = (InternalContext) context;
       |  }
       |
       |  private <T> T getGrpcClient(Class<T> serviceClass) {
       |    return context.getComponentGrpcClient(serviceClass);
       |  }
       |
       |  private <Req, Res> SingleResponseRequestBuilder<Req, Res> addHeaders(SingleResponseRequestBuilder<Req, Res> requestBuilder, Metadata metadata){
       |    SingleResponseRequestBuilder<Req, Res> updatedBuilder = requestBuilder;
       |    for (Metadata.MetadataEntry entry: metadata){
       |      if (entry.isText()) {
       |        updatedBuilder = updatedBuilder.addHeader(entry.getKey(), entry.getValue());
       |      }
       |    }
       |    return updatedBuilder;
       |  }
       |
       |  ${Format.indent(componentGetters, 2)}
       |
       |  ${Format.indent(componentCallImpls, 2)}
       |}
       |""".stripMargin
  }

  private def fullyQualifiedMessage(messageType: ProtoMessageType): String =
    s"${messageType.parent.javaPackage}.${messageType.fullName}"

  private def nameFor(component: Service): String = {
    component match {
      case as: ActionService => as.className.split('.').last
      case es: EntityService => es.componentFullName.split('.').last
      case vs: ViewService   => vs.className.split('.').last
    }
  }

  private def callableCommandsFor(service: Service): Iterable[ModelBuilder.Command] =
    service match {
      case view: ViewService =>
        // only queries, not update commands for views
        view.queries.filter(_.isUnary)
      case _ =>
        // only unary commands for now
        service.commands.filter(_.isUnary)
    }
}
