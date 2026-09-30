/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.codegen.java

import kalix.codegen.ModelBuilder
import kalix.codegen.PackageNaming
import kalix.codegen.ProtoMessageType
import kalix.codegen.TestData.protoMessageType

class WorkflowSourceGeneratorSuite extends munit.FunSuite {

  private def packaging(protoFile: String, outerClass: String, protoPackage: String): PackageNaming =
    PackageNaming(protoFile, outerClass, protoPackage, None, Some(outerClass))

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
    val workflowFile = packaging("example/workflow/workflow.proto", "WorkflowApi", "example.workflow")
    val handlerFile = packaging("example/handler/handler.proto", "HandlerApi", "example.handler")
    val eventFile = packaging("example/event/event_msgs.proto", "EventMsgs", "example.event")
    val emptyFile = PackageNaming(
      "google/protobuf/empty.proto",
      "EmptyProto",
      "google.protobuf",
      Some("com.google.protobuf"),
      Some("EmptyProto"),
      javaMultipleFiles = true)

    val start = protoMessageType(workflowFile, "Start")
    val workflowState = protoMessageType(workflowFile, "WorkflowState")
    val somethingHappened = protoMessageType(eventFile, "SomethingHappened")
    val empty = protoMessageType(emptyFile, "Empty")

    // handler.proto only declares the service, all its rpc message types come from other files
    val handlerService = ModelBuilder.ActionService(
      protoMessageType(handlerFile, "EventHandler"),
      Seq(command("Handle", somethingHappened, empty)),
      None)

    val workflowService = ModelBuilder.EntityService(
      protoMessageType(workflowFile, "ExampleWorkflowService"),
      Seq(command("Start", start, empty)),
      "example.workflow.ExampleWorkflow")

    val workflowComponent = ModelBuilder.WorkflowComponent(
      protoMessageType(workflowFile, "ExampleWorkflow"),
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
    val workflowFile = packaging("example/workflow/workflow.proto", "WorkflowApi", "example.workflow")
    val firstFile = packaging("example/first/user_api.proto", "UserApi", "example.first")
    val secondFile = packaging("example/second/user_api.proto", "UserApi", "example.second")
    val emptyFile = PackageNaming(
      "google/protobuf/empty.proto",
      "EmptyProto",
      "google.protobuf",
      Some("com.google.protobuf"),
      Some("EmptyProto"),
      javaMultipleFiles = true)

    val start = protoMessageType(workflowFile, "Start")
    val workflowState = protoMessageType(workflowFile, "WorkflowState")
    val empty = protoMessageType(emptyFile, "Empty")

    // two services in different packages whose files have the same outer class name and define no messages
    def actionService(name: String, file: PackageNaming) =
      ModelBuilder.ActionService(
        protoMessageType(file, name),
        Seq(command("Handle", protoMessageType(workflowFile, "Request"), empty)),
        None)

    val workflowService = ModelBuilder.EntityService(
      protoMessageType(workflowFile, "ExampleWorkflowService"),
      Seq(command("Start", start, empty)),
      "example.workflow.ExampleWorkflow")

    val workflowComponent = ModelBuilder.WorkflowComponent(
      protoMessageType(workflowFile, "ExampleWorkflow"),
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

  test("workflow provider keeps an outer class in its own package apart from a same-named one in another package") {
    val workflowFile = packaging("example/workflow/workflow.proto", "WorkflowApi", "example.workflow")
    // same outer class name, one in the workflow's own package and one in another package
    val localFile = packaging("example/workflow/user_api.proto", "UserApi", "example.workflow")
    val otherFile = packaging("example/other/user_api.proto", "UserApi", "example.other")
    val emptyFile = PackageNaming(
      "google/protobuf/empty.proto",
      "EmptyProto",
      "google.protobuf",
      Some("com.google.protobuf"),
      Some("EmptyProto"),
      javaMultipleFiles = true)

    val start = protoMessageType(workflowFile, "Start")
    val workflowState = protoMessageType(workflowFile, "WorkflowState")
    val empty = protoMessageType(emptyFile, "Empty")

    // the files only declare a service, the rpc message types come from the workflow's file
    def actionService(name: String, file: PackageNaming) =
      ModelBuilder.ActionService(protoMessageType(file, name), Seq(command("Handle", start, empty)), None)

    val workflowService = ModelBuilder.EntityService(
      protoMessageType(workflowFile, "ExampleWorkflowService"),
      Seq(command("Start", start, empty)),
      "example.workflow.ExampleWorkflow")

    val workflowComponent = ModelBuilder.WorkflowComponent(
      protoMessageType(workflowFile, "ExampleWorkflow"),
      "example-workflow",
      ModelBuilder.State(workflowState))

    val source = WorkflowSourceGenerator.workflowProvider(
      workflowService,
      workflowComponent,
      "example.workflow",
      "ExampleWorkflow",
      Seq(workflowService, actionService("LocalHandler", localFile), actionService("OtherHandler", otherFile)))

    // importing example.other.UserApi would shadow the UserApi in the same package, dropping its descriptor
    assert(
      !source.contains("import example.other.UserApi;"),
      s"example.other.UserApi must not be imported, it shadows the UserApi in the same package:\n$source")
    assert(
      source.contains("example.other.UserApi.getDescriptor()"),
      s"expected a fully qualified reference to example.other.UserApi:\n$source")
    assert(
      source.contains("      UserApi.getDescriptor()"),
      s"expected the descriptor of the UserApi in the same package:\n$source")
  }
}
