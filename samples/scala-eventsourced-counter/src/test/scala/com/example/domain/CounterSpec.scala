package com.example.domain

import kalix.scalasdk.Metadata
import kalix.scalasdk.eventsourcedentity.EventSourcedEntity
import kalix.scalasdk.testkit.EventSourcedResult
import com.example
import com.google.protobuf.empty.Empty
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class CounterSpec extends AnyWordSpec with Matchers {
  "The Counter" should {
    "have example test that can be removed" in {
      val testKit = CounterTestKit(new Counter(_))
      // use the testkit to execute a command:
      // val result: EventSourcedResult[R] = testKit.someOperation(SomeRequest("id"));
      // verify the emitted events
      // val actualEvent: ExpectedEvent = result.nextEventOfType[ExpectedEvent]
      // actualEvent shouldBe expectedEvent
      // verify the final state after applying the events
      // testKit.state() shouldBe expectedState
      // verify the response
      // result.reply shouldBe expectedReply
      // verify the final state after the command
    }

    "correctly process commands of type Increase" in {
      val testKit = CounterTestKit(new Counter(_))
      // val result: EventSourcedResult[Empty] = testKit.increase(example.IncreaseValue(...))
    }

    "correctly process commands of type IncreaseWithSideEffect" in {
      val testKit = CounterTestKit(new Counter(_))
      val result: EventSourcedResult[Empty] = testKit.increaseWithSideEffect(example.IncreaseValue("id1",1))
      val actualEvent = result.nextEvent[ValueIncreased]
      actualEvent shouldBe ValueIncreased(value = 1)
      val sideEffect = result.sideEffects.head
      sideEffect.methodName shouldBe "Increase"
      sideEffect.serviceName shouldBe "com.example.CounterService"
    }

     "correctly process commands of type IncreaseWithConditional" in {
      val testKit = CounterTestKit(new Counter(_))
      val result: EventSourcedResult[Empty] = testKit.increaseWithConditional(example.IncreaseValue("id1",1),Metadata.empty.set("myKey","myValue"))
      val actualEvent = result.nextEvent[ValueIncreased]
      actualEvent shouldBe ValueIncreased(value = 2)
    }

    "correctly process commands of type Decrease" in {
      val testKit = CounterTestKit(new Counter(_))
      // val result: EventSourcedResult[Empty] = testKit.decrease(example.DecreaseValue(...))
    }

    "correctly process commands of type Reset" in {
      val testKit = CounterTestKit(new Counter(_))
      // val result: EventSourcedResult[Empty] = testKit.reset(example.ResetValue(...))
    }

    "correctly process commands of type GetCurrentCounter" in {
      val testKit = CounterTestKit(new Counter(_))
      // val result: EventSourcedResult[example.CurrentCounter] = testKit.getCurrentCounter(example.GetCounter(...))
    }
  }
}
