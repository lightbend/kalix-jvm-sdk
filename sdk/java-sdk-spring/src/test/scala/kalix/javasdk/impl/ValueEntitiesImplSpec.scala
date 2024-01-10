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

package kalix.javasdk.impl

import com.google.protobuf.ByteString._
import com.google.protobuf.any.{ Any => ScalaPbAny }
import kalix.javasdk.impl.valueentity.TestValueService
import kalix.javasdk.valueentity.ReflectiveValueEntityProvider
import kalix.javasdk.valueentity.TestVEState0
import kalix.javasdk.valueentity.TestVEState1
import kalix.javasdk.valueentity.TestVEState2
import kalix.javasdk.valueentity.TestValueEntity
import kalix.javasdk.valueentity.TestValueEntityMigration
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
  }

  private def emptySyntheticRequest(methodName: String) = {
    ScalaPbAny(s"type.googleapis.com/kalix.javasdk.valueentity.${methodName}KalixSyntheticRequest", EMPTY)
  }
}
