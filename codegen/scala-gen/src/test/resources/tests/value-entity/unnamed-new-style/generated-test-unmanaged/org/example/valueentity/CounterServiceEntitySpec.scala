package org.example.valueentity

import com.google.protobuf.empty.Empty
import kalix.scalasdk.testkit.ValueEntityResult
import kalix.scalasdk.valueentity.ValueEntity
import org.example.valueentity
import org.example.valueentity.domain.CounterState
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CounterServiceEntitySpec
    extends AnyWordSpec
    with Matchers {

  "CounterServiceEntity" must {

    "have example test that can be removed" in {
      val service = CounterServiceEntityTestKit(new CounterServiceEntity(_))
      pending
      // use the testkit to execute a command
      // and verify final updated state:
      // val result = service.someOperation(SomeRequest)
      // verify the reply
      // val reply = result.getReply()
      // reply shouldBe expectedReply
      // verify the final state after the command
      // service.currentState() shouldBe expectedState
    }

    "handle command Increase" in {
      val service = CounterServiceEntityTestKit(new CounterServiceEntity(_))
      pending
      // val result = service.increase(IncreaseValue(...))
    }

    "handle command Decrease" in {
      val service = CounterServiceEntityTestKit(new CounterServiceEntity(_))
      pending
      // val result = service.decrease(DecreaseValue(...))
    }

  }
}
