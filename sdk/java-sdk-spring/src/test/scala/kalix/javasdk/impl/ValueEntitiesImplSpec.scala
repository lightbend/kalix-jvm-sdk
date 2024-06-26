/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl

import com.google.protobuf.ByteString._
import com.google.protobuf.any.{ Any => ScalaPbAny }
import com.typesafe.config.ConfigFactory
import kalix.javasdk.impl.telemetry.{ Telemetry, TraceInstrumentation }
import kalix.javasdk.impl.valueentity.TestValueService
import kalix.javasdk.valueentity._
import kalix.protocol.component.{ Metadata, MetadataEntry }
import kalix.testkit.TestProtocol
import kalix.testkit.valueentity.ValueEntityMessages
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ValueEntitiesImplSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll {
  import ValueEntityMessages._

  "EntityImpl" should {

    "recover entity when state name has been changed" in {
      val entityId = "1"
      val jsonMessageCodec = new JsonMessageCodec()
      val service = new TestValueService(
        ReflectiveValueEntityProvider
          .of[TestVEState1, TestValueEntity](classOf[TestValueEntity], jsonMessageCodec, _ => new TestValueEntity()))
      val protocol = TestProtocol(service.port)
      val entity = protocol.valueEntity.connect()
      //old state
      entity.send(
        init(
          classOf[TestValueEntity].getName,
          entityId,
          jsonMessageCodec.encodeJava(new TestVEState0("old-state", 12))))

      entity.send(command(1, entityId, "Get", emptySyntheticRequest("Get")))
      //new state
      entity.expect(reply(1, jsonMessageCodec.encodeJava(new TestVEState1("old-state", 12))))
      protocol.terminate()
      service.terminate()
    }

    "recover entity when state change has non-backward compatible change" in {
      val entityId = "1"
      val jsonMessageCodec = new JsonMessageCodec()

      val service: TestValueService = new TestValueService(
        ReflectiveValueEntityProvider
          .of[TestVEState2, TestValueEntityMigration](
            classOf[TestValueEntityMigration],
            jsonMessageCodec,
            _ => new TestValueEntityMigration()))
      val protocol: TestProtocol = TestProtocol(service.port)
      val entity = protocol.valueEntity.connect()
      //old state
      entity.send(
        init(
          classOf[TestValueEntityMigration].getName,
          entityId,
          jsonMessageCodec.encodeJava(new TestVEState0("old-state", 12))))

      entity.send(command(1, entityId, "Get", emptySyntheticRequest("Get")))
      //migrated state
      entity.expect(reply(1, jsonMessageCodec.encodeJava(new TestVEState2("old-state", 12, "newValue"))))

      protocol.terminate()
      service.terminate()
    }

    "Add the trace_id to the MDC" in {
      val entityId = "1"
      val jsonMessageCodec = new JsonMessageCodec()

      val service: TestValueService = new TestValueService(
        ReflectiveValueEntityProvider
          .of[TestVEState1, TestValueEntity](classOf[TestValueEntity], jsonMessageCodec, _ => new TestValueEntity()),
        Some(ConfigFactory.parseString(s"${TraceInstrumentation.TRACING_ENDPOINT}=\"http://fakeurl:1234\"")))
      val protocol: TestProtocol = TestProtocol(service.port)
      val entity = protocol.valueEntity.connect()
      //old state
      entity.send(
        init(
          classOf[TestValueEntity].getName,
          entityId,
          jsonMessageCodec.encodeJava(new TestVEState0("some-state", 1))))

      val traceParent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
      val metadata = Metadata(Seq(MetadataEntry("traceparent", MetadataEntry.Value.StringValue(traceParent))))

      service.expectLogMdc(Map(Telemetry.TRACE_ID -> "4bf92f3577b34da6a3ce929d0e0e4736")) {
        entity.send(command(1, entityId, "Get", emptySyntheticRequest("Get"), Option(metadata)))
      }
    }
  }

  private def emptySyntheticRequest(methodName: String) = {
    ScalaPbAny(s"type.googleapis.com/kalix.javasdk.valueentity.${methodName}KalixSyntheticRequest", EMPTY)
  }
}
