/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.codegen

import com.google.protobuf.DescriptorProtos.DescriptorProto
import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto
import com.google.protobuf.DescriptorProtos.MethodOptions
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto
import com.google.protobuf.DescriptorProtos.ServiceOptions
import com.google.protobuf.Descriptors
import kalix.ActionDef
import kalix.CodegenOptions
import kalix.DirectDestination
import kalix.DirectSource
import kalix.EventSource
import kalix.Eventing
import kalix.ServiceEventing
import kalix.ServiceEventingOut
import kalix.StartFrom
import kalix.ViewDef

class StartFromValidationSuite extends munit.FunSuite {

  private implicit val log: Log = new Log {
    override def debug(message: String): Unit = ()
    override def warn(message: String): Unit = ()
    override def info(message: String): Unit = ()
  }
  private implicit val extractor: ModelBuilder.ProtoMessageTypeExtractor = TestProtoMessageTypeExtractor

  private val latest = StartFrom.newBuilder.setLatest(true).build()
  private val timestamp =
    StartFrom.newBuilder.setTimestamp(com.google.protobuf.Timestamp.newBuilder.setSeconds(1000)).build()
  private val empty = StartFrom.getDefaultInstance

  private def entity(startFrom: Option[StartFrom]): EventSource = {
    val b = EventSource.newBuilder.setEventSourcedEntity("counter")
    startFrom.foreach(b.setStartFrom)
    b.build()
  }

  private def valueEntity(startFrom: StartFrom): EventSource =
    EventSource.newBuilder.setValueEntity("counter").setStartFrom(startFrom).build()

  private def direct(startFrom: StartFrom): EventSource =
    EventSource.newBuilder
      .setDirect(DirectSource.newBuilder.setService("other").setEventStreamId("stream"))
      .setStartFrom(startFrom)
      .build()

  private def topic(startFrom: Option[StartFrom]): EventSource = {
    val b = EventSource.newBuilder.setTopic("topic")
    startFrom.foreach(b.setStartFrom)
    b.build()
  }

  /** An event source without a source, which uses the source of the service. */
  private def inheriting(startFrom: StartFrom): EventSource = EventSource.newBuilder.setStartFrom(startFrom).build()

  private def methodOptions(in: Option[EventSource], view: Boolean): MethodOptions = {
    val options = kalix.MethodOptions.newBuilder
    in.foreach(i => options.setEventing(Eventing.newBuilder.setIn(i)))
    if (view) options.setView(kalix.View.newBuilder.setUpdate(kalix.View.Update.newBuilder.setTable("t")))
    MethodOptions.newBuilder.setExtension(kalix.Annotations.method, options.build()).build()
  }

  private def build(
      view: Boolean,
      legacy: Boolean,
      serviceIn: Option[EventSource],
      methodIns: Seq[Option[EventSource]],
      directOut: Boolean = false): ModelBuilder.Model = {
    val serviceOptions = ServiceOptions.newBuilder
    if (legacy) {
      val eventing = ServiceEventing.newBuilder
      serviceIn.foreach(eventing.setIn)
      if (directOut)
        eventing.setOut(ServiceEventingOut.newBuilder.setDirect(DirectDestination.newBuilder.setEventStreamId("s")))
      val kind =
        if (view) kalix.ServiceOptions.ServiceType.SERVICE_TYPE_VIEW
        else kalix.ServiceOptions.ServiceType.SERVICE_TYPE_ACTION
      serviceOptions.setExtension(
        kalix.Annotations.service,
        kalix.ServiceOptions.newBuilder.setType(kind).setEventing(eventing).build())
    } else {
      val codegen =
        if (view) CodegenOptions.newBuilder.setView(ViewDef.newBuilder).build()
        else CodegenOptions.newBuilder.setAction(ActionDef.newBuilder).build()
      serviceOptions.setExtension(kalix.Annotations.codegen, codegen)
      if (serviceIn.isDefined || directOut) {
        val eventing = ServiceEventing.newBuilder
        serviceIn.foreach(eventing.setIn)
        if (directOut)
          eventing.setOut(ServiceEventingOut.newBuilder.setDirect(DirectDestination.newBuilder.setEventStreamId("s")))
        serviceOptions.setExtension(
          kalix.Annotations.service,
          kalix.ServiceOptions.newBuilder.setEventing(eventing).build())
      }
    }
    val service = ServiceDescriptorProto.newBuilder.setName("Svc").setOptions(serviceOptions)
    methodIns.zipWithIndex.foreach { case (in, i) =>
      // a view needs an update method
      val viewIn = if (view && in.isEmpty) Some(entity(None)) else in
      service.addMethod(
        MethodDescriptorProto.newBuilder
          .setName(s"Consume$i")
          .setInputType(".test.Msg")
          .setOutputType(".test.Msg")
          .setOptions(methodOptions(viewIn, view)))
    }
    val file = FileDescriptorProto.newBuilder
      .setName("test.proto")
      .setPackage("test")
      .addMessageType(DescriptorProto.newBuilder.setName("Msg"))
      .addService(service)
      .build()
    ModelBuilder.introspectProtobufClasses(Seq(Descriptors.FileDescriptor.buildFrom(file, Array.empty)))
  }

  private def refused(expected: String)(body: => Any): Unit = {
    val error = intercept[IllegalArgumentException](body)
    assert(error.getMessage.contains(expected), error.getMessage)
  }

  for ((path, legacy) <- Seq("codegen options" -> false, "service options" -> true)) {

    test(s"accept start_from on actions consuming entities and other services ($path)") {
      assertEquals(build(view = false, legacy, None, Seq(Some(entity(Some(latest))))).services.size, 1)
      assertEquals(build(view = false, legacy, None, Seq(Some(valueEntity(timestamp)))).services.size, 1)
      assertEquals(build(view = false, legacy, None, Seq(Some(direct(latest)))).services.size, 1)
      assertEquals(build(view = false, legacy, Some(entity(Some(latest))), Seq(None)).services.size, 1)
    }

    test(s"accept an action without start_from ($path)") {
      assertEquals(build(view = false, legacy, None, Seq(Some(entity(None)))).services.size, 1)
    }

    test(s"accept the same start_from on methods consuming the same source ($path)") {
      val methods = Seq(Some(entity(Some(latest))), Some(entity(Some(latest))), Some(entity(None)))
      assertEquals(build(view = false, legacy, None, methods).services.size, 1)
    }

    test(s"refuse start_from on a view, on service and method level ($path)") {
      refused("Svc.Consume0")(build(view = true, legacy, None, Seq(Some(entity(Some(latest))))))
      refused("test.Svc")(build(view = true, legacy, Some(entity(Some(latest))), Seq(None)))
    }

    test(s"accept a view without start_from, and an empty start_from ($path)") {
      assertEquals(build(view = true, legacy, None, Seq(None)).services.size, 1)
      assertEquals(build(view = true, legacy, None, Seq(Some(entity(Some(empty))))).services.size, 1)
    }

    test(s"refuse start_from on a topic source ($path)") {
      refused("Svc.Consume0")(build(view = false, legacy, None, Seq(Some(topic(Some(latest))))))
      refused("test.Svc")(build(view = false, legacy, Some(topic(Some(latest))), Seq(None)))
    }

    test(s"refuse start_from on a method that uses a topic of the service ($path)") {
      refused("Svc.Consume0")(build(view = false, legacy, Some(topic(None)), Seq(Some(inheriting(latest)))))
    }

    test(s"accept an empty start_from on a topic ($path)") {
      assertEquals(build(view = false, legacy, None, Seq(Some(topic(Some(empty))))).services.size, 1)
    }

    test(s"refuse start_from on service level and method level ($path)") {
      refused("also sets it on service level")(
        build(view = false, legacy, Some(entity(Some(latest))), Seq(Some(inheriting(latest)))))
    }

    test(s"refuse different start_from for methods consuming the same source ($path)") {
      val methods = Seq(Some(entity(Some(latest))), Some(entity(Some(timestamp))))
      refused("different 'start_from'")(build(view = false, legacy, None, methods))
    }

    test(s"accept different start_from for methods consuming different sources ($path)") {
      val methods = Seq(Some(entity(Some(latest))), Some(valueEntity(timestamp)))
      assertEquals(build(view = false, legacy, None, methods).services.size, 1)
    }

    test(s"refuse start_from on a consumer that publishes to a service stream ($path)") {
      refused("publishes to a service stream")(
        build(view = false, legacy, Some(entity(Some(latest))), Seq(None), directOut = true))
    }
  }
}
