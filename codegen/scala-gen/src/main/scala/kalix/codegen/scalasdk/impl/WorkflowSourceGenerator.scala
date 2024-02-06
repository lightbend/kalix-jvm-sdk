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

package kalix.codegen.scalasdk.impl

import kalix.codegen.ClassMessageType
import kalix.codegen.File
import kalix.codegen.ModelBuilder
import kalix.codegen.PackageNaming
import kalix.codegen.ProtoMessageType

object WorkflowSourceGenerator {
  import ScalaGeneratorUtils._
  import kalix.codegen.SourceGeneratorUtils._

  def generateUnmanaged(workflow: ModelBuilder.WorkflowComponent, service: ModelBuilder.EntityService): Seq[File] =
    Seq(generateImplementationSkeleton(workflow, service))

  def generateManaged(
      workflowComponent: ModelBuilder.WorkflowComponent,
      service: ModelBuilder.EntityService,
      mainPackageName: PackageNaming,
      allServices: Seq[ModelBuilder.Service]): Seq[File] =
    Seq(
      abstractWorkflow(workflowComponent, service, mainPackageName),
      handler(workflowComponent, service),
      provider(workflowComponent, service, allServices))

  private[codegen] def abstractWorkflow(
      workflowComponent: ModelBuilder.WorkflowComponent,
      service: ModelBuilder.EntityService,
      mainPackageName: PackageNaming): File = {
    import Types.Workflow._

    val stateType = workflowComponent.state.messageType
    val abstractWorkflowName = workflowComponent.abstractEntityName

    val methods = service.commands
      .map { cmd =>
        val methodName = cmd.name

        val inputType = cmd.inputType
        val outputType = cmd.outputType

        c"""|def ${lowerFirst(methodName)}(currentState: $stateType, ${lowerFirst(cmd.inputType.name)}: $inputType): $AbstractWorkflow.Effect[$outputType]
            |"""

      }

    val Components = ClassMessageType(mainPackageName.javaPackage + ".Components")
    val ComponentsImpl = ClassMessageType(mainPackageName.javaPackage + ".ComponentsImpl")

    generate(
      workflowComponent.messageType.parent,
      abstractWorkflowName,
      c"""|$managedComment
          |
          |abstract class $abstractWorkflowName extends $ProtoWorkflow[$stateType] {
          |
          |  def components: $Components =
          |    new ${ComponentsImpl}(commandContext())
          |
          |  $methods
          |}
          |""",
      packageImports = Seq(service.messageType.parent))
  }

  private[codegen] def handler(
      workflowComponent: ModelBuilder.WorkflowComponent,
      service: ModelBuilder.EntityService): File = {
    import Types.Workflow._
    val stateType = workflowComponent.state.messageType
    val workflowName = workflowComponent.messageType

    val commandCases = service.commands
      .map { cmd =>
        val methodName = cmd.name
        val inputType = cmd.inputType
        c"""|case "$methodName" =>
            |  workflow.${lowerFirst(methodName)}(state, command.asInstanceOf[$inputType])
            |"""
      }

    generate(
      workflowComponent.messageType.parent,
      workflowComponent.routerName,
      c"""|$managedComment
          |
          |/**
          | * A workflow handler that is the glue between the Protobuf service and actual workflow implementation.
          | */
          |class ${workflowName.name}Router(workflow: $workflowName) extends $WorkflowRouter[$stateType, $workflowName](workflow) {
          |  def handleCommand(commandName: String, state: $stateType, command: Any, context: $CommandContext): $AbstractWorkflow.Effect[_] = {
          |    commandName match {
          |      $commandCases
          |      case _ =>
          |        throw new $CommandHandlerNotFound(commandName)
          |    }
          |  }
          |}
          |""",
      packageImports = Seq(service.messageType.parent))
  }

  def provider(
      workflowComponent: ModelBuilder.WorkflowComponent,
      service: ModelBuilder.EntityService,
      allServices: Seq[ModelBuilder.Service]): File = {
    import Types.Descriptors
    import Types.ImmutableSeq
    import Types.Workflow._
    val className = workflowComponent.providerName

    val potentialTypesThatWorkflowCanUse: Seq[ProtoMessageType] = allServices.flatMap(_.commandTypes)
    val relevantTypes = allRelevantMessageTypes(service, workflowComponent) ++ potentialTypesThatWorkflowCanUse
    val relevantProtoTypes = relevantTypes.collect { case proto: ProtoMessageType => proto }

    val relevantDescriptors = relevantProtoTypes.map(_.descriptorImport).distinct.sortBy(_.name)

    generate(
      workflowComponent.messageType.parent,
      className,
      c"""|$managedComment
          |
          |object $className {
          |  def apply(workflowFactory: $WorkflowContext => ${workflowComponent.messageType.name}): $className =
          |    new $className(workflowFactory, $WorkflowOptions.defaults)
          |}
          |class $className private(workflowFactory: $WorkflowContext => ${workflowComponent.messageType.name}, override val options: $WorkflowOptions)
          |  extends $WorkflowProvider[${workflowComponent.state.messageType}, ${workflowComponent.messageType}] {
          |
          |  def withOptions(newOptions: $WorkflowOptions): $className =
          |    new $className(workflowFactory, newOptions)
          |
          |  override final val serviceDescriptor: $Descriptors.ServiceDescriptor =
          |    ${service.messageType.descriptorImport}.javaDescriptor.findServiceByName("${service.messageType.protoName}")
          |
          |  override final val typeId: String = "${workflowComponent.typeId}"
          |
          |  override final def newRouter(context: $WorkflowContext): ${workflowComponent.routerName} =
          |    new ${workflowComponent.routerName}(workflowFactory(context))
          |
          |  override final val additionalDescriptors: $ImmutableSeq[$Descriptors.FileDescriptor] =
          |    ${relevantDescriptors.map(d => c"$d.javaDescriptor :: ")}Nil
          |}
          |""",
      packageImports = Seq(service.messageType.parent))
  }

  def generateImplementationSkeleton(
      workflowComponent: ModelBuilder.WorkflowComponent,
      service: ModelBuilder.EntityService): File = {
    import Types.Workflow._

    val methods = service.commands.map { cmd =>
      c"""|override def ${lowerFirst(cmd.name)}(currentState: ${workflowComponent.state.messageType}, ${lowerFirst(
        cmd.inputType.name)}: ${cmd.inputType}): $AbstractWorkflow.Effect[${cmd.outputType}] =
          |  effects.error("The command handler for `${cmd.name}` is not implemented, yet")
          |"""
    }

    generate(
      workflowComponent.messageType.parent,
      workflowComponent.messageType.name,
      c"""|$unmanagedComment
          |
          |class ${workflowComponent.messageType.name}(context: $WorkflowContext) extends ${workflowComponent.abstractEntityName} {
          |  override def emptyState: ${workflowComponent.state.messageType} =
          |    throw new UnsupportedOperationException("Not implemented yet, replace with your empty workflow state")
          |
          |  override def definition: AbstractWorkflow.WorkflowDef[${workflowComponent.state.messageType}] =
          |    throw new UnsupportedOperationException("Not implemented yet, replace with your workflow definition")
          |
          |  $methods
          |}
          |""",
      packageImports = Seq(service.messageType.parent))
  }
}
