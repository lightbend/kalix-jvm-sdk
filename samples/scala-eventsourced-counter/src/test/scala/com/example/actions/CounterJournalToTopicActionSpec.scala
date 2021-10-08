package com.example.actions

import com.example.domain.ValueDecreased
import com.example.domain.ValueIncreased
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

// tag::class[]
class CounterJournalToTopicActionSpec extends AnyWordSpec with Matchers {

  "CounterJournalToTopicAction" must {

    // end::class[]
    "have example test that can be removed" in {
      val testKit = CounterJournalToTopicActionTestKit(new CounterJournalToTopicAction(_))
      // use the testkit to execute a command
      // and verify final updated state:
      // val result = testKit.someOperation(SomeRequest)
      // verify the response
      // result.reply() shouldBe expectedResponse
    }

    // tag::class[]
    "handle command Increase" in {
      val testKit = CounterJournalToTopicActionTestKit(new CounterJournalToTopicAction(_)) // <1>
      val result = testKit.increase(ValueIncreased(1)) // <2>
      result.reply shouldBe Increased(1) // <3>

    }
    // end::class[]

    "handle command Decrease" in {
      val testKit = CounterJournalToTopicActionTestKit(new CounterJournalToTopicAction(_))
      // val result = testKit.decrease(ValueDecreased.defaultInstance)
    }

    "handle command Ignore" in {
      val testKit = CounterJournalToTopicActionTestKit(new CounterJournalToTopicAction(_))
      // val result = testKit.ignore(com.google.protobuf.Any.getDefaultInstance)
    }

  }
}