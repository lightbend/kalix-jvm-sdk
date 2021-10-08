package com.example.actions

import com.akkaserverless.scalasdk.action.Action
import com.akkaserverless.scalasdk.testkit.ActionResult
import com.example.actions
import com.google.protobuf.empty.Empty
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CounterTopicSubscriptionActionSpec
    extends AnyWordSpec
    with Matchers {

  "CounterTopicSubscriptionAction" must {

    "have example test that can be removed" in {
      val testKit = CounterTopicSubscriptionActionTestKit(new CounterTopicSubscriptionAction(_))
      // use the testkit to execute a command
      // and verify final updated state:
      // val result = testKit.someOperation(SomeRequest)
      // verify the response
      // result.reply shouldBe expectedReply
    }

    "handle command Increase" in {
      val testKit = CounterTopicSubscriptionActionTestKit(new CounterTopicSubscriptionAction(_))
      // val result = testKit.increase(Increased(...))
    }

    "handle command Decrease" in {
      val testKit = CounterTopicSubscriptionActionTestKit(new CounterTopicSubscriptionAction(_))
      // val result = testKit.decrease(Decreased(...))
    }

  }
}
