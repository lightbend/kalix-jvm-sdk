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

package kalix.javasdk.impl.eventsourceentity

import kalix.javasdk.JsonSupport
import kalix.javasdk.impl.JsonMessageCodec
import kalix.javasdk.impl.eventsourcedentity.EventSourcedHandlersExtractor
import kalix.spring.testmodels.eventsourcedentity.EmployeeEvent.EmployeeEmailUpdated
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.CounterEventSourcedEntity
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.EmployeeEntityWithMissingHandler
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.ErrorDuplicatedEventsEntity
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.ErrorWrongSignaturesEntity
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class EventSourcedHandlersExtractorSpec extends AnyWordSpec with Matchers {

  private final val messageCodec = new JsonMessageCodec
  private final val intTypeUrl = messageCodec.typeUrlFor(classOf[Integer])
  private final val eventTypeUrl = messageCodec.typeUrlFor(classOf[CounterEventSourcedEntity.Event])
  private final val additionalMappingTypeUrl = JsonSupport.KALIX_JSON + "additional-mapping"

  "EventSourcedHandlersExtractor" should {

    "extract public well-annotated handlers keyed by event type received as unique parameter" in {
      val result = EventSourcedHandlersExtractor.handlersFrom(classOf[CounterEventSourcedEntity], messageCodec)
      result.handlers.size shouldBe 3
      result.handlers.get(intTypeUrl).map { m =>
        m.method.getName shouldBe "receivedIntegerEvent"
        m.method.getParameterCount shouldBe 1
      }
      result.handlers.get(eventTypeUrl).map { m =>
        m.method.getName shouldBe "receiveStringEvent"
        m.method.getParameterCount shouldBe 1
      }
      //additional type pointing to the same handler to support events schema evolution
      result.handlers.get(additionalMappingTypeUrl).map { m =>
        m.method.getName shouldBe "receiveStringEvent"
        m.method.getParameterCount shouldBe 1
      }
      result.errors shouldBe empty
    }

    "report error on annotated handlers with wrong return type or number of params" in {
      val result = EventSourcedHandlersExtractor.handlersFrom(classOf[ErrorWrongSignaturesEntity], messageCodec)
      result.handlers shouldBe empty
      result.errors.size shouldBe 1
      val offendingMethods = result.errors.flatMap(_.methods.map(_.getName).sorted)
      offendingMethods shouldBe List("receivedIntegerEvent", "receivedIntegerEventAndString")
    }

    //TODO remove ignore after updating to Scala 2.13.11 (https://github.com/scala/scala/pull/10105)
    "report error on missing event handler for sealed event interface" ignore {
      val result = EventSourcedHandlersExtractor.handlersFrom(classOf[EmployeeEntityWithMissingHandler], messageCodec)
      result.handlers.size shouldBe 1
      result.errors.size shouldBe 1
      val offendingMethods = result.errors.flatMap(_.methods.map(_.getName).sorted)
      offendingMethods shouldBe empty
      val missingHandlerFor = result.errors.flatMap(_.missingHandlersFor)
      missingHandlerFor should contain only classOf[EmployeeEmailUpdated]
    }

    "report error on annotated handlers with duplicates signatures (receiving the same event type)" in {
      val result = EventSourcedHandlersExtractor.handlersFrom(classOf[ErrorDuplicatedEventsEntity], messageCodec)
      result.handlers.size shouldBe 1
      result.handlers.get(intTypeUrl).map { m =>
        m.method.getName shouldBe "receivedIntegerEvent"
        m.method.getParameterCount shouldBe 1
      }

      result.errors.size shouldBe 1
      val offendingMethods = result.errors.flatMap(_.methods.map(_.getName).sorted)
      offendingMethods shouldBe List("receivedIntegerEvent", "receivedIntegerEventDup")
    }
  }
}
