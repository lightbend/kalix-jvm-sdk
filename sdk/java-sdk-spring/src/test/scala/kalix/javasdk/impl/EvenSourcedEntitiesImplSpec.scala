/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl

import com.google.protobuf.ByteString._
import com.google.protobuf.any.{ Any => ScalaPbAny }
import kalix.javasdk.JsonSupport
import kalix.javasdk.eventsourced.ReflectiveEventSourcedEntityProvider
import kalix.javasdk.eventsourcedentity.OldTestESEvent.OldEvent1
import kalix.javasdk.eventsourcedentity.OldTestESEvent.OldEvent2
import kalix.javasdk.eventsourcedentity.OldTestESEvent.OldEvent3
import kalix.javasdk.eventsourcedentity.TestESEvent
import kalix.javasdk.eventsourcedentity.TestESEvent.Event4
import kalix.javasdk.eventsourcedentity.TestESState
import kalix.javasdk.eventsourcedentity.TestEventSourcedEntity
import kalix.javasdk.impl.eventsourcedentity.TestEventSourcedService
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

  private def emptySyntheticRequest(methodName: String) = {
    ScalaPbAny(s"type.googleapis.com/kalix.javasdk.eventsourcedentity.${methodName}KalixSyntheticRequest", EMPTY)
  }
}
