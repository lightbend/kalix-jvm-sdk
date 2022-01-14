package org.example.valueentity

import com.akkaserverless.scalasdk.testkit.ValueEntityResult
import com.akkaserverless.scalasdk.valueentity.ValueEntity
import com.google.protobuf.empty.Empty
import org.example.valueentity
import org.example.valueentity.domain.CounterState
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CounterServiceEntitySpec
    extends AnyWordSpec
    with Matchers {

  "CounterServiceEntity" must {

    "have example test that can be removed" in {
      val testKit = CounterServiceEntityTestKit(new CounterServiceEntity(_))
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
      val testKit = CounterServiceEntityTestKit(new CounterServiceEntity(_))
      // val result = testKit.increase(IncreaseValue(...))
    }

    "handle command Decrease" in {
      val testKit = CounterServiceEntityTestKit(new CounterServiceEntity(_))
      // val result = testKit.decrease(DecreaseValue(...))
    }

  }
}
