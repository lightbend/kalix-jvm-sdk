package org.example.domain

import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity
import com.akkaserverless.scalasdk.testkit.EventSourcedResult
import com.google.protobuf.empty.Empty
import org.example.eventsourcedentity.counter_api
import org.example.state.counter_state.CounterState
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
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
      // val result: EventSourcedResult[Empty] = testKit.increase(counter_api.IncreaseValue(...))
    }

    "correctly process commands of type Decrease" in {
      val testKit = CounterTestKit(new Counter(_))
      // val result: EventSourcedResult[Empty] = testKit.decrease(counter_api.DecreaseValue(...))
    }
  }
}
