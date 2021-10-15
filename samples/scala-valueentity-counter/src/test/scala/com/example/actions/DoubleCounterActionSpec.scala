package com.example.actions

import com.akkaserverless.scalasdk.action.Action
import com.akkaserverless.scalasdk.testkit.ActionResult
import com.example.IncreaseValue
import com.google.protobuf.empty.Empty
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import com.akkaserverless.scalasdk.testkit.ServiceCallDetails

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class DoubleCounterActionSpec
    extends AnyWordSpec
    with Matchers {

  // tag::side-effect-test[]
  "DoubleCounterAction" must {
    "handle command IncreaseWithSideEffect" in {
      val testKit = DoubleCounterActionTestKit(new DoubleCounterAction(_))

      val result: ActionResult[Empty] = testKit.increaseWithSideEffect(IncreaseValue(value = 1))// <1>
      result.reply shouldBe Empty.defaultInstance

      val sideEffect = result.sideEffects.head // <2>
      sideEffect.getServiceName shouldBe "com.example.CounterService" // <3>
      sideEffect.getMethodName shouldBe "Increase" // <4>
      sideEffect.getMessage shouldBe IncreaseValue(value = 2) // <5>
    }
  }
  // end::side-effect-test[]
}
