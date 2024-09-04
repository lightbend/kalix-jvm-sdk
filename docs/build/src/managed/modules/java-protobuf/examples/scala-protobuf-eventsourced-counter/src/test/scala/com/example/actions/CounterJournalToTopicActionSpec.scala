package com.example.actions

import com.example.domain.ValueDecreased
import com.example.domain.ValueIncreased
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import kalix.scalasdk.Metadata

class CounterJournalToTopicActionSpec extends AnyWordSpec with Matchers {

  "CounterJournalToTopicAction" must {

    "handle command Increase" in {
      val testKit = CounterJournalToTopicActionTestKit(new CounterJournalToTopicAction(_))
      val result = testKit.onIncreased(ValueIncreased(1))
      result.reply shouldBe Increased(1)
    }

    "handle command Decrease" in {
      val testKit = CounterJournalToTopicActionTestKit(new CounterJournalToTopicAction(_))
      val result = testKit.onDecreased(ValueDecreased(1))
      result.reply shouldBe Decreased(1)
    }

  }
}
