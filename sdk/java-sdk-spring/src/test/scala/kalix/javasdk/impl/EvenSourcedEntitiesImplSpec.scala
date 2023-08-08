/*
 * Copyright 2021 Lightbend Inc.
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

package kalix.javasdk.impl

import com.google.protobuf.ByteString._
import com.google.protobuf.any.{Any => ScalaPbAny}
import kalix.javasdk.eventsourced.ReflectiveEventSourcedEntityProvider
import kalix.javasdk.eventsourcedentity.OldTestESEvent.OldEvent1
import kalix.javasdk.eventsourcedentity.OldTestESEvent.OldEvent2
import kalix.javasdk.eventsourcedentity.OldTestESEvent.OldEvent3
import kalix.javasdk.eventsourcedentity.TestESEvent
import kalix.javasdk.eventsourcedentity.TestESState
import kalix.javasdk.eventsourcedentity.TestEventSourcedEntity
import kalix.javasdk.impl.eventsourcedentity.TestEventSourcedService
//import kalix.javasdk.impl.valueentity.TestValueService
//import kalix.javasdk.valueentity.ReflectiveValueEntityProvider
//import kalix.javasdk.valueentity.TestVEState0
//import kalix.javasdk.valueentity.TestVEState1
//import kalix.javasdk.valueentity.TestVEState2
//import kalix.javasdk.valueentity.TestValueEntity
//import kalix.javasdk.valueentity.TestValueEntityMigration
import kalix.testkit.TestProtocol
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class EvenSourcedEntitiesImplSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll {

  import kalix.testkit.eventsourcedentity.EventSourcedMessages._

  "EntityImpl" should {

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
      entity.send(event(2, jsonMessageCodec.encodeJava(new OldEvent3(true))))

      entity.send(command(1, entityId, "Get", emptySyntheticRequest("Get")))
      //321 because of Event2Migration
      entity.expect(reply(1, jsonMessageCodec.encodeJava(new TestESState("state", 321, true))))
      protocol.terminate()
      service.terminate()
    }
  }

  private def emptySyntheticRequest(methodName: String) = {
    ScalaPbAny(s"type.googleapis.com/kalix.javasdk.eventsourcedentity.${methodName}KalixSyntheticRequest", EMPTY)
  }
}
