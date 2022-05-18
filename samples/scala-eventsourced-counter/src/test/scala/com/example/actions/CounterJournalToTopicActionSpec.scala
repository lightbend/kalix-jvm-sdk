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
      val result = testKit.increase(ValueIncreased(1))
      result.reply shouldBe Increased(1)
    }

    "handle command Increase with Metadata" in {
      val testKit = CounterJournalToTopicActionTestKit(new CounterJournalToTopicAction(_))
      val result = testKit.increaseConditional(ValueIncreased(1),Metadata.empty.set("myKey","myValue").set("ce-subject","mySubject"))
      result.reply shouldBe Increased(2)
    }

    "handle command Decrease" in {
      val testKit = CounterJournalToTopicActionTestKit(new CounterJournalToTopicAction(_))
      val result = testKit.decrease(ValueDecreased(1))
      result.reply shouldBe Decreased(1)
    }

  }
}
