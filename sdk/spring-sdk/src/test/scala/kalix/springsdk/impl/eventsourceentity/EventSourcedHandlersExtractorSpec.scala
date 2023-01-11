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

package kalix.springsdk.impl.eventsourceentity

import kalix.springsdk.impl.SpringSdkMessageCodec
import kalix.springsdk.impl.eventsourcedentity.EventSourcedHandlersExtractor
import kalix.springsdk.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.{
  CounterEventSourcedEntity,
  ErrorDuplicatedEventsEntity,
  ErrorWrongSignaturesEntity
}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class EventSourcedHandlersExtractorSpec extends AnyWordSpec with Matchers {

  private final val messageCodec = new SpringSdkMessageCodec
  private final val intTypeUrl = messageCodec.typeUrlFor(classOf[Integer])
  private final val stringTypeUrl = messageCodec.typeUrlFor(classOf[String])

  "EventSourcedHandlersExtractor" should {

    "extract public well-annotated handlers keyed by event type received as unique parameter" in {
      val result = EventSourcedHandlersExtractor.handlersFrom(classOf[CounterEventSourcedEntity], messageCodec)
      result.handlers.size shouldBe 2
      result.handlers.get(intTypeUrl).map { m =>
        m.method.getName shouldBe "receivedIntegerEvent"
        m.method.getParameterCount shouldBe 1
      }
      result.handlers.get(stringTypeUrl).map { m =>
        m.method.getName shouldBe "receiveStringEvent"
        m.method.getParameterCount shouldBe 1
      }
      result.errors shouldBe empty
    }

    "report error on annotated handlers with wrong return type or number of params" in {
      val result = EventSourcedHandlersExtractor.handlersFrom(classOf[ErrorWrongSignaturesEntity], messageCodec)
      result.handlers shouldBe empty
      result.errors.size shouldBe 1
      val offendingMethods = result.errors.map(_.methods.map(_.getName).sorted)
      offendingMethods shouldBe List(List("receivedIntegerEvent", "receivedIntegerEventAndString"))
    }

    "report error on annotated handlers with duplicates signatures (receiving the same event type)" in {
      val result = EventSourcedHandlersExtractor.handlersFrom(classOf[ErrorDuplicatedEventsEntity], messageCodec)
      result.handlers.size shouldBe 1
      result.handlers.get(intTypeUrl).map { m =>
        m.method.getName shouldBe "receivedIntegerEvent"
        m.method.getParameterCount shouldBe 1
      }

      result.errors.size shouldBe 1
      val offendingMethods = result.errors.map(_.methods.map(_.getName).sorted)
      offendingMethods shouldBe List(List("receivedIntegerEvent", "receivedIntegerEventDup"))
    }
  }
}