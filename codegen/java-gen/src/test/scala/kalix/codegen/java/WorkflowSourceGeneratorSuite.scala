/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.codegen.java

import kalix.codegen.ModelBuilder
import kalix.codegen.PackageNaming
import kalix.codegen.ProtoMessageType

class WorkflowSourceGeneratorSuite extends munit.FunSuite {

  // mirrors ProtoMessageTypeExtractor: the descriptor object is the file's outer class
  private def protoType(name: String, parent: PackageNaming): ProtoMessageType =
    ProtoMessageType(
      name,
      name,
      parent,
      Some(
        ProtoMessageType(
          parent.javaOuterClassname,
          parent.javaOuterClassname,
          parent.copy(javaOuterClassnameOption = None, javaMultipleFiles = true),
          None)))

  private def command(name: String, in: ProtoMessageType, out: ProtoMessageType) =
    ModelBuilder.Command(
      name,
      in,
      out,
      streamedInput = false,
      streamedOutput = false,
      inFromTopic = false,
      outToTopic = false,
      ignore = false,
      handleDeletes = false,
      handleSnapshots = false,
      viewTable = "")

  // https://support.akka.io case #16497
  test("workflow provider imports the outer class of other services defined in a different package and file") {
    val workflowFile = PackageNaming("example/workflow/workflow.proto", "WorkflowApi", "example.workflow")
    val handlerFile = PackageNaming("example/handler/handler.proto", "HandlerApi", "example.handler")
    val eventFile = PackageNaming("example/event/event_msgs.proto", "EventMsgs", "example.event")
    val emptyFile = PackageNaming(
      "google/protobuf/empty.proto",
      "EmptyProto",
      "google.protobuf",
      Some("com.google.protobuf"),
      Some("EmptyProto"),
      javaMultipleFiles = true)

    val start = protoType("Start", workflowFile)
    val workflowState = protoType("WorkflowState", workflowFile)
    val somethingHappened = protoType("SomethingHappened", eventFile)
    val empty = protoType("Empty", emptyFile)

    // handler.proto only declares the service, all its rpc message types come from other files
    val handlerService = ModelBuilder.ActionService(
      protoType("EventHandler", handlerFile),
      Seq(command("Handle", somethingHappened, empty)),
      None)

    val workflowService = ModelBuilder.EntityService(
      protoType("ExampleWorkflowService", workflowFile),
      Seq(command("Start", start, empty)),
      "example.workflow.ExampleWorkflow")

    val workflowComponent = ModelBuilder.WorkflowComponent(
      protoType("ExampleWorkflow", workflowFile),
      "example-workflow",
      ModelBuilder.State(workflowState))

    val source = WorkflowSourceGenerator.workflowProvider(
      workflowService,
      workflowComponent,
      "example.workflow",
      "ExampleWorkflow",
      Seq(workflowService, handlerService))

    assert(
      source.contains("HandlerApi.getDescriptor()"),
      s"expected additionalDescriptors() to reference HandlerApi:\n$source")
    // the generated class lives in example.workflow, so HandlerApi (example.handler) must be imported
    assert(
      source.contains("import example.handler.HandlerApi;"),
      s"HandlerApi is referenced but not imported, generated code will not compile:\n$source")
  }

  test("workflow provider fully qualifies service outer classes with clashing simple names") {
    val workflowFile = PackageNaming("example/workflow/workflow.proto", "WorkflowApi", "example.workflow")
    val firstFile = PackageNaming("example/first/user_api.proto", "UserApi", "example.first")
    val secondFile = PackageNaming("example/second/user_api.proto", "UserApi", "example.second")
    val emptyFile = PackageNaming(
      "google/protobuf/empty.proto",
      "EmptyProto",
      "google.protobuf",
      Some("com.google.protobuf"),
      Some("EmptyProto"),
      javaMultipleFiles = true)

    val start = protoType("Start", workflowFile)
    val workflowState = protoType("WorkflowState", workflowFile)
    val empty = protoType("Empty", emptyFile)

    // two services in different packages whose files have the same outer class name and define no messages
    def actionService(name: String, file: PackageNaming) =
      ModelBuilder.ActionService(
        protoType(name, file),
        Seq(command("Handle", protoType("Request", workflowFile), empty)),
        None)

    val workflowService = ModelBuilder.EntityService(
      protoType("ExampleWorkflowService", workflowFile),
      Seq(command("Start", start, empty)),
      "example.workflow.ExampleWorkflow")

    val workflowComponent = ModelBuilder.WorkflowComponent(
      protoType("ExampleWorkflow", workflowFile),
      "example-workflow",
      ModelBuilder.State(workflowState))

    val source = WorkflowSourceGenerator.workflowProvider(
      workflowService,
      workflowComponent,
      "example.workflow",
      "ExampleWorkflow",
      Seq(workflowService, actionService("FirstHandler", firstFile), actionService("SecondHandler", secondFile)))

    // a bare `UserApi` would be ambiguous (and is not imported), so both must be fully qualified
    assert(
      source.contains("example.first.UserApi.getDescriptor()"),
      s"expected a fully qualified reference to example.first.UserApi:\n$source")
    assert(
      source.contains("example.second.UserApi.getDescriptor()"),
      s"expected a fully qualified reference to example.second.UserApi:\n$source")
    assert(!source.contains("      UserApi.getDescriptor()"), s"unexpected unqualified reference to UserApi:\n$source")
    assert(!source.contains("import example.first.UserApi;"), s"clashing outer class must not be imported:\n$source")
    assert(!source.contains("import example.second.UserApi;"), s"clashing outer class must not be imported:\n$source")
  }
}
