package com.example.actions

import com.akkaserverless.scalasdk.action.Action
import com.akkaserverless.scalasdk.testkit.ActionResult
import com.example.IncreaseValue
import com.google.protobuf.empty.Empty
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class DoubleCounterActionSpec
    extends AnyWordSpec
    with Matchers {

  "DoubleCounterAction" must {

    "have example test that can be removed" in {
      val testKit = DoubleCounterActionTestKit(new DoubleCounterAction(_))
      // use the testkit to execute a command
      // and verify final updated state:
      // val result = testKit.someOperation(SomeRequest)
      // verify the response
      // result.reply shouldBe expectedReply
    }

    "handle command Increase" in {
      val testKit = DoubleCounterActionTestKit(new DoubleCounterAction(_))
      // val result = testKit.increase(IncreaseValue(...))
    }

  }
}
