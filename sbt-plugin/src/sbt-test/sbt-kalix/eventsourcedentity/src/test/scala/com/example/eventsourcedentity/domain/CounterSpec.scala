package com.example.eventsourcedentity.domain

import kalix.scalasdk.eventsourcedentity.EventSourcedEntity
import kalix.scalasdk.testkit.EventSourcedResult
import com.example.eventsourcedentity
import com.google.protobuf.empty.Empty
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class CounterSpec extends AnyWordSpec with Matchers {
  val counterId = "37"

  "The Counter" should {
    "correctly process commands of type Increase" in {
      val testKit = CounterTestKit(new Counter(_))

      testKit.increase(eventsourcedentity.IncreaseValue(counterId, 42))
      val result1 = testKit.getCurrentCounter(eventsourcedentity.GetCounter(counterId))
      result1.reply.value shouldBe(42)

      testKit.increase(eventsourcedentity.IncreaseValue(counterId, 15))
      val result2 = testKit.getCurrentCounter(eventsourcedentity.GetCounter(counterId))
      result2.reply.value shouldBe(42+15)
    }

    "correctly process commands of type Decrease" in {
      val testKit = CounterTestKit(new Counter(_))

      testKit.increase(eventsourcedentity.IncreaseValue(counterId, 42))
      val result1 = testKit.getCurrentCounter(eventsourcedentity.GetCounter(counterId))
      result1.reply.value shouldBe(42)

      testKit.decrease(eventsourcedentity.DecreaseValue(counterId, 15))
      val result2 = testKit.getCurrentCounter(eventsourcedentity.GetCounter(counterId))
      result2.reply.value shouldBe(42-15)
    }

    "correctly process commands of type Reset" in {
      val testKit = CounterTestKit(new Counter(_))

      testKit.increase(eventsourcedentity.IncreaseValue(counterId, 42))
      val result1 = testKit.getCurrentCounter(eventsourcedentity.GetCounter(counterId))
      result1.reply.value shouldBe(42)

      val reset = testKit.reset(eventsourcedentity.ResetValue(counterId))
      reset.nextEvent[Decreased].value shouldBe(42)

      val result2 = testKit.getCurrentCounter(eventsourcedentity.GetCounter(counterId))
      result2.reply.value shouldBe(0)
    }
  }
}
