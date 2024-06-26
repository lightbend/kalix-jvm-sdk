/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl

import com.google.protobuf.ByteString._
import com.google.protobuf.any.{ Any => ScalaPbAny }
import com.typesafe.config.ConfigFactory
import kalix.javasdk.JsonSupport
import kalix.javasdk.eventsourced.ReflectiveEventSourcedEntityProvider
import kalix.javasdk.eventsourcedentity.OldTestESEvent.{ OldEvent1, OldEvent2, OldEvent3 }
import kalix.javasdk.eventsourcedentity.TestESEvent.Event4
import kalix.javasdk.eventsourcedentity.{ TestESEvent, TestESState, TestEventSourcedEntity }
import kalix.javasdk.impl.eventsourcedentity.TestEventSourcedService
import kalix.javasdk.impl.telemetry.{ Telemetry, TraceInstrumentation }
import kalix.protocol.component.{ Metadata, MetadataEntry }
import kalix.testkit.TestProtocol
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class EvenSourcedEntitiesImplSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll {

  import kalix.testkit.eventsourcedentity.EventSourcedMessages._

  "EventSourcedEntityImpl" should {

    "recover es state based on old events version" in {
      val entityId = "1"
      val jsonMessageCodec = new JsonMessageCodec()
      val service = new TestEventSourcedService(
        ReflectiveEventSourcedEntityProvider
          .of[TestESState, TestESEvent, TestEventSourcedEntity](
            classOf[TestEventSourcedEntity],
            new JsonMessageCodec(),
            _ => new TestEventSourcedEntity()))
      val protocol = TestProtocol(service.port)
      val entity = protocol.eventSourced.connect()

      entity.send(init(classOf[TestEventSourcedEntity].getName, entityId))
      entity.send(event(1, jsonMessageCodec.encodeJava(new OldEvent1("state"))))
      entity.send(event(2, jsonMessageCodec.encodeJava(new OldEvent2(123))))
      entity.send(event(3, jsonMessageCodec.encodeJava(new OldEvent3(true))))
      entity.send(
        event(4, JsonSupport.encodeJson(new Event4("value"), classOf[Event4].getName + "#1"))
      ) //current version is 2

      entity.send(command(1, entityId, "Get", emptySyntheticRequest("Get")))
      //321 because of Event2Migration
      entity.expect(reply(1, jsonMessageCodec.encodeJava(new TestESState("state", 321, true, "value-v2"))))
      protocol.terminate()
      service.terminate()
    }
  }

  "inject traces correctly into metadata and keeps trace_id in MDC" in {
    val entityId = "1"
    val service = new TestEventSourcedService(
      ReflectiveEventSourcedEntityProvider
        .of[TestESState, TestESEvent, TestEventSourcedEntity](
          classOf[TestEventSourcedEntity],
          new JsonMessageCodec(),
          _ => new TestEventSourcedEntity()),
      Some(ConfigFactory.parseString(s"${TraceInstrumentation.TRACING_ENDPOINT}=\"http://fakeurl:1234\"")))
    val protocol = TestProtocol(service.port)
    val entity = protocol.eventSourced.connect()

    entity.send(init(classOf[TestEventSourcedEntity].getName, entityId))
    val traceParent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
    val metadata = Metadata(Seq(MetadataEntry("traceparent", MetadataEntry.Value.StringValue(traceParent))))

    service.expectLogMdc(Map(Telemetry.TRACE_ID -> "4bf92f3577b34da6a3ce929d0e0e4736")) {
      entity.send(command(1, entityId, "Get", emptySyntheticRequest("Get"), Option(metadata)))
    }
  }

  private def emptySyntheticRequest(methodName: String) = {
    ScalaPbAny(s"type.googleapis.com/kalix.javasdk.eventsourcedentity.${methodName}KalixSyntheticRequest", EMPTY)
  }
}
