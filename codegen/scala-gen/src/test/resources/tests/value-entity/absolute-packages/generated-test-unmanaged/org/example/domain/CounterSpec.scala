package org.example.domain

import com.akkaserverless.scalasdk.testkit.ValueEntityResult
import com.akkaserverless.scalasdk.valueentity.ValueEntity
import com.google.protobuf.empty.Empty
import org.example.state.CounterState
import org.example.valueentity
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CounterSpec
    extends AnyWordSpec
    with Matchers {

  "Counter" must {

    "have example test that can be removed" in {
      val testKit = CounterTestKit(new Counter(_))
      // use the testkit to execute a command
      // and verify final updated state:
      // val result = testKit.someOperation(SomeRequest)
      // verify the response
      // val actualResponse = result.getReply()
      // actualResponse shouldBe expectedResponse
      // verify the final state after the command
      // testKit.currentState() shouldBe expectedState
    }

    "handle command Increase" in {
      val testKit = CounterTestKit(new Counter(_))
      // val result = testKit.increase(valueentity.IncreaseValue(...))
    }

    "handle command Decrease" in {
      val testKit = CounterTestKit(new Counter(_))
      // val result = testKit.decrease(valueentity.DecreaseValue(...))
    }

  }
}
