package com.example.actions

import kalix.scalasdk.action.Action
import kalix.scalasdk.testkit.ActionResult
import com.example.domain.CounterState
import com.google.protobuf.empty.Empty
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class CounterStateSubscriptionActionSpec
    extends AnyWordSpec
    with Matchers {

  "CounterStateSubscriptionAction" must {

    "have example test that can be removed" in {
      val testKit = CounterStateSubscriptionActionTestKit(new CounterStateSubscriptionAction(_))
      // use the testkit to execute a command
      // and verify final updated state:
      // val result = testKit.someOperation(SomeRequest)
      // verify the response
      // result.reply shouldBe expectedReply
    }

    "handle command OnUpdateState" in {
      val testKit = CounterStateSubscriptionActionTestKit(new CounterStateSubscriptionAction(_))
      // val result = testKit.onUpdateState(CounterState(...))
    }

  }
}
