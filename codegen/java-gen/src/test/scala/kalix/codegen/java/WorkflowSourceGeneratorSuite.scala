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

  private val workflowFile = packaging("example/workflow/workflow.proto", "WorkflowApi", "example.workflow")
  private val emptyFile = PackageNaming(
    "google/protobuf/empty.proto",
    "EmptyProto",
    "google.protobuf",
    Some("com.google.protobuf"),
    Some("EmptyProto"),
    javaMultipleFiles = true)

  private val start = protoMessageType(workflowFile, "Start")
  private val empty = protoMessageType(emptyFile, "Empty")

  // a service that only declares the service, all its rpc message types come from other files
  private def actionService(name: String, file: PackageNaming, input: ProtoMessageType = start) =
    ModelBuilder.ActionService(protoMessageType(file, name), Seq(command("Handle", input, empty)), None)

  private val workflowState = protoMessageType(workflowFile, "WorkflowState")

  /** The provider generated for a workflow in `example.workflow`, in a project that also has the other services. */
  private def generateProvider(otherServices: ModelBuilder.Service*): String =
    generateProviderWithState(workflowState, otherServices)

  private def generateProviderWithState(state: ProtoMessageType, otherServices: Seq[ModelBuilder.Service]): String = {
    val workflowService = ModelBuilder.EntityService(
      protoMessageType(workflowFile, "ExampleWorkflowService"),
      Seq(command("Start", start, empty)),
      "example.workflow.ExampleWorkflow")

    val workflowComponent = ModelBuilder.WorkflowComponent(
      protoMessageType(workflowFile, "ExampleWorkflow"),
      "example-workflow",
      ModelBuilder.State(state))

    WorkflowSourceGenerator.workflowProvider(
      workflowService,
      workflowComponent,
      "example.workflow",
      "ExampleWorkflow",
      workflowService +: otherServices)
  }

  private def importsOf(source: String): Seq[String] =
    source.linesIterator.map(_.trim).filter(_.startsWith("import ")).toSeq

  /** The entries of `additionalDescriptors()`, independent of the template's indentation. */
  private def additionalDescriptorsOf(source: String): Seq[String] =
    source.linesIterator
      .map(_.trim)
      .dropWhile(!_.startsWith("return new Descriptors.FileDescriptor[]"))
      .drop(1)
      .takeWhile(_ != "};")
      .map(_.stripSuffix(","))
      .toSeq

  // https://support.akka.io case #16497
  test("workflow provider imports the outer class of other services defined in a different package and file") {
    val handlerFile = packaging("example/handler/handler.proto", "HandlerApi", "example.handler")
    val eventFile = packaging("example/event/event_msgs.proto", "EventMsgs", "example.event")

    val source = generateProvider(
      actionService("EventHandler", handlerFile, input = protoMessageType(eventFile, "SomethingHappened")))

    assert(
      additionalDescriptorsOf(source).contains("HandlerApi.getDescriptor()"),
      s"expected additionalDescriptors() to reference HandlerApi:\n$source")
    // the generated class lives in example.workflow, so HandlerApi (example.handler) must be imported
    assert(
      importsOf(source).contains("import example.handler.HandlerApi;"),
      s"HandlerApi is referenced but not imported, generated code will not compile:\n$source")
  }

  test("workflow provider fully qualifies service outer classes with clashing simple names") {
    val firstFile = packaging("example/first/user_api.proto", "UserApi", "example.first")
    val secondFile = packaging("example/second/user_api.proto", "UserApi", "example.second")

    val source = generateProvider(actionService("FirstHandler", firstFile), actionService("SecondHandler", secondFile))

    // a bare `UserApi` would be ambiguous (and is not imported), so both must be fully qualified
    val descriptors = additionalDescriptorsOf(source)
    assert(descriptors.contains("example.first.UserApi.getDescriptor()"), s"$descriptors\n$source")
    assert(descriptors.contains("example.second.UserApi.getDescriptor()"), s"$descriptors\n$source")
    assert(!descriptors.contains("UserApi.getDescriptor()"), s"unexpected unqualified UserApi:\n$source")
    assert(!importsOf(source).exists(_.endsWith(".UserApi;")), s"clashing outer class must not be imported:\n$source")
  }

  test("workflow provider keeps an outer class in its own package apart from a same-named one in another package") {
    val localFile = packaging("example/workflow/user_api.proto", "UserApi", "example.workflow")
    val otherFile = packaging("example/other/user_api.proto", "UserApi", "example.other")

    val source = generateProvider(actionService("LocalHandler", localFile), actionService("OtherHandler", otherFile))

    // importing example.other.UserApi would shadow the UserApi in the same package, dropping its descriptor
    val descriptors = additionalDescriptorsOf(source)
    assert(!importsOf(source).exists(_.endsWith(".UserApi;")), s"UserApi must not be imported:\n$source")
    assert(descriptors.contains("example.other.UserApi.getDescriptor()"), s"$descriptors\n$source")
    assert(descriptors.contains("UserApi.getDescriptor()"), s"expected the UserApi in the same package:\n$source")
  }

  test("workflow provider fully qualifies a state type that clashes with the outer class of another service") {
    // a java_multiple_files state type, named like the outer class of a service file in another package
    val stateFile = packaging("example/state/state.proto", "StateApi", "example.state").copy(javaMultipleFiles = true)
    val otherFile = packaging("example/other/shared.proto", "Shared", "example.other")

    val source =
      generateProviderWithState(protoMessageType(stateFile, "Shared"), Seq(actionService("OtherHandler", otherFile)))

    assert(
      source.contains("implements WorkflowProvider<example.state.Shared, ExampleWorkflow>"),
      s"expected the state type to be fully qualified:\n$source")
  }
}
