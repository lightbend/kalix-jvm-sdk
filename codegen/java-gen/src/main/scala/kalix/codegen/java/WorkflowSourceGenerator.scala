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
import kalix.codegen.SourceGeneratorUtils.allRelevantMessageTypes
import kalix.codegen.SourceGeneratorUtils.collectRelevantTypes
import kalix.codegen.SourceGeneratorUtils.generateImports
import kalix.codegen.SourceGeneratorUtils.lowerFirst
import kalix.codegen.SourceGeneratorUtils.managedComment
import kalix.codegen.SourceGeneratorUtils.qualifiedType
import kalix.codegen.SourceGeneratorUtils.unmanagedComment
import kalix.codegen.java.JavaGeneratorUtils.typeName
import kalix.codegen.java.JavaGeneratorUtils.writeImports

object WorkflowSourceGenerator {

  private[codegen] def workflowSource(
      service: ModelBuilder.EntityService,
      workflowComponent: ModelBuilder.WorkflowComponent,
      packageName: String,
      className: String): String = {

    val imports = generateImports(
      allRelevantMessageTypes(service, workflowComponent),
      packageName,
      otherImports = Seq("kalix.javasdk.workflow.WorkflowContext"))

    val stateType = workflowComponent.state.messageType.fullName

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
       |${unmanagedComment(Right(workflowComponent))}
       |
       |public class $className extends Abstract$className {
       |  @SuppressWarnings("unused")
       |  private final String workflowId;
       |
       |  public $className(WorkflowContext context) {
       |    this.workflowId = context.workflowId();
       |  }
       |
       |  @Override
       |  public $stateType emptyState() {
       |    throw new UnsupportedOperationException("Not implemented yet, replace with your empty workflow state");
       |  }
       |
       |  @Override
       |  public WorkflowDef<$stateType> definition() {
       |    throw new UnsupportedOperationException("Not implemented yet, replace with your workflow definition");
       |  }
       |
       |  ${Format.indent(methods, num = 2)}
       |}
       |""".stripMargin
  }

  private[codegen] def abstractWorkflowComponent(
      service: ModelBuilder.EntityService,
      workflowComponent: ModelBuilder.WorkflowComponent,
      packageName: String,
      className: String,
      mainPackageName: String): String = {

    val stateType = workflowComponent.state.messageType

    implicit val imports = generateImports(
      allRelevantMessageTypes(service, workflowComponent),
      packageName,
      otherImports =
        Seq("kalix.javasdk.workflow.ProtoWorkflow", s"$mainPackageName.Components", s"$mainPackageName.ComponentsImpl"))

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
       |public abstract class Abstract$className extends ProtoWorkflow<${typeName(stateType)}> {
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

  private[codegen] def workflowRouter(
      service: ModelBuilder.EntityService,
      workflowComponent: ModelBuilder.WorkflowComponent,
      packageName: String,
      className: String): String = {

    val imports = generateImports(
      allRelevantMessageTypes(service, workflowComponent),
      packageName,
      otherImports = Seq(
        "kalix.javasdk.workflow.CommandContext",
        "kalix.javasdk.workflow.Workflow",
        "kalix.javasdk.impl.workflow.WorkflowRouter"))

    val stateType = workflowComponent.state.messageType.fullName

    val commandCases = service.commands
      .map { cmd =>
        val methodName = cmd.name
        val inputType = cmd.inputType.fullName
        s"""|case "$methodName":
            |  return workflow().${lowerFirst(methodName)}(state, ($inputType) command);
            |""".stripMargin
      }

    s"""package $packageName;
       |
       |${writeImports(imports)}
       |
       |$managedComment
       |
       |/**
       | * A workflow handler that is the glue between the Protobuf service <code>${service.messageType.name}</code>
       | * and the command handler methods in the <code>${workflowComponent.messageType.name}</code> class.
       | */
       |public class ${className}Router extends WorkflowRouter<$stateType, ${workflowComponent.messageType.name}> {
       |
       |  public ${className}Router(${workflowComponent.messageType.name} workflow) {
       |    super(workflow);
       |  }
       |
       |  @Override
       |  public Workflow.Effect<?> handleCommand(
       |      String commandName, $stateType state, Object command, CommandContext context) {
       |    switch (commandName) {
       |
       |      ${Format.indent(commandCases, 6)}
       |
       |      default:
       |        throw new WorkflowRouter.CommandHandlerNotFound(commandName);
       |    }
       |  }
       |}
       |""".stripMargin

  }

  private[codegen] def workflowProvider(
      service: ModelBuilder.EntityService,
      workflowComponent: ModelBuilder.WorkflowComponent,
      packageName: String,
      className: String,
      allServices: Seq[ModelBuilder.Service]): String = {

    val potentialTypesThatWorkflowCanUse: Seq[ProtoMessageType] = allServices.flatMap(_.commandTypes)
    val relevantTypes = allRelevantMessageTypes(service, workflowComponent) ++ potentialTypesThatWorkflowCanUse
    val relevantProtoTypes = relevantTypes.collect { case proto: ProtoMessageType => proto }

    implicit val imports = generateImports(
      relevantTypes ++ relevantProtoTypes.flatMap(_.descriptorObject),
      packageName,
      otherImports = Seq(
        "kalix.javasdk.workflow.WorkflowContext",
        "kalix.javasdk.workflow.WorkflowOptions",
        "kalix.javasdk.workflow.WorkflowProvider",
        "com.google.protobuf.Descriptors",
        "java.util.function.Function"))

    val relevantTypeDescriptors =
      collectRelevantTypes(relevantProtoTypes, service.messageType)
        .flatMap(_.descriptorObject)
        .map { messageType => s"${messageType.name}.getDescriptor()" }

    //in the workflow definition we can potentially call any GRPC service, so we need to collect all service descriptors
    val allServicesDescriptors = allServices.flatMap(AdditionalDescriptors.collectServiceDescriptors)

    val descriptors =
      (relevantTypeDescriptors ++ allServicesDescriptors).distinct.sorted

    s"""package $packageName;
       |
       |${writeImports(imports)}
       |
       |$managedComment
       |
       |/**
       | * A workflow provider that defines how to register and create the workflow for
       | * the Protobuf service <code>${service.messageType.name}</code>.
       | *
       | * Should be used with the <code>register</code> method in {@link kalix.javasdk.Kalix}.
       | */
       |public class ${className}Provider implements WorkflowProvider<${workflowComponent.state.messageType.fullName}, $className> {
       |
       |  private final Function<WorkflowContext, $className> workflowFactory;
       |  private final WorkflowOptions options;
       |
       |  /** Factory method of ${className}Provider */
       |  public static ${className}Provider of(Function<WorkflowContext, $className> workflowFactory) {
       |    return new ${className}Provider(workflowFactory, WorkflowOptions.defaults());
       |  }
       |
       |  private ${className}Provider(
       |      Function<WorkflowContext, $className> workflowFactory,
       |      WorkflowOptions options) {
       |    this.workflowFactory = workflowFactory;
       |    this.options = options;
       |  }
       |
       |  @Override
       |  public final WorkflowOptions options() {
       |    return options;
       |  }
       |
       |  public final ${className}Provider withOptions(WorkflowOptions options) {
       |    return new ${className}Provider(workflowFactory, options);
       |  }
       |
       |  @Override
       |  public final Descriptors.ServiceDescriptor serviceDescriptor() {
       |    return ${typeName(service.messageType.descriptorImport)}.getDescriptor().findServiceByName("${service.messageType.name}");
       |  }
       |
       |  @Override
       |  public final String typeId() {
       |    return "${workflowComponent.typeId}";
       |  }
       |
       |  @Override
       |  public final ${className}Router newRouter(WorkflowContext context) {
       |    return new ${className}Router(workflowFactory.apply(context));
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

}
