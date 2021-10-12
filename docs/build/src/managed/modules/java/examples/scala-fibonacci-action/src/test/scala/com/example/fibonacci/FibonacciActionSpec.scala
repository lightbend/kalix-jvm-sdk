package com.example.fibonacci

import com.akkaserverless.scalasdk.action.Action
import com.akkaserverless.scalasdk.testkit.ActionResult
import com.example.fibonacci
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

// tag::class[]
class FibonacciActionSpec
    extends AnyWordSpec
    with Matchers {

  "FibonacciAction" must {

    "handle command NextNumber" in {
      val testKit = FibonacciActionTestKit(new FibonacciAction(_)) // <1>
      val result = testKit.nextNumber(Number(5)) // <2>
      result.reply shouldBe (Number(8)) // <3>
    }

  }
}
// end::class[]
