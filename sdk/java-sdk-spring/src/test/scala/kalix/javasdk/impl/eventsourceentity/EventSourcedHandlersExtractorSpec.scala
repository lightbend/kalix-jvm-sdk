/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.eventsourceentity

import kalix.javasdk.JsonSupport
import kalix.javasdk.impl.JsonMessageCodec
import kalix.javasdk.impl.eventsourcedentity.EventSourcedHandlersExtractor
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.CounterEventSourcedEntity
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.EmployeeEntity
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class EventSourcedHandlersExtractorSpec extends AnyWordSpec with Matchers {

  private final val messageCodec = new JsonMessageCodec
  private final val intTypeUrl = messageCodec.typeUrlFor(classOf[Integer])
  private final val eventTypeUrl = messageCodec.typeUrlFor(classOf[CounterEventSourcedEntity.Event])
  private final val additionalMappingTypeUrl = JsonSupport.KALIX_JSON + "additional-mapping"

  "EventSourcedHandlersExtractor" should {

    "extract handlers for sealed interface handler" in {
      val result = EventSourcedHandlersExtractor.handlersFrom(classOf[EmployeeEntity], messageCodec)
      result.size shouldBe 3
      result.get(JsonSupport.KALIX_JSON + "created").map { m =>
        m.method.getName shouldBe "onEvent"
        m.method.getParameterCount shouldBe 1
      }
      result.get(JsonSupport.KALIX_JSON + "old-created").map { m =>
        m.method.getName shouldBe "onEvent"
        m.method.getParameterCount shouldBe 1
      }
      result.get(JsonSupport.KALIX_JSON + "emailUpdated").map { m =>
        m.method.getName shouldBe "onEvent"
        m.method.getParameterCount shouldBe 1
      }
    }

    "extract public well-annotated handlers keyed by event type received as unique parameter" in {
      val result = EventSourcedHandlersExtractor.handlersFrom(classOf[CounterEventSourcedEntity], messageCodec)
      result.size shouldBe 4
      result.get(JsonSupport.KALIX_JSON + "int").map { m =>
        m.method.getName shouldBe "receivedIntegerEvent"
        m.method.getParameterCount shouldBe 1
      }
      result.get(intTypeUrl).map { m =>
        m.method.getName shouldBe "receivedIntegerEvent"
        m.method.getParameterCount shouldBe 1
      }
      result.get(eventTypeUrl).map { m =>
        m.method.getName shouldBe "receiveStringEvent"
        m.method.getParameterCount shouldBe 1
      }
      //additional type pointing to the same handler to support events schema evolution
      result.get(additionalMappingTypeUrl).map { m =>
        m.method.getName shouldBe "receiveStringEvent"
        m.method.getParameterCount shouldBe 1
      }
    }
  }
}
