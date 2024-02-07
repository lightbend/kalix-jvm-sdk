package org.example.service

import com.google.protobuf.empty.Empty
import kalix.scalasdk.action.Action
import kalix.scalasdk.testkit.ActionResult
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class SomeServiceActionSpec
    extends AnyWordSpec
    with Matchers {

  "SomeServiceAction" must {

    "have example test that can be removed" in {
      val service = SomeServiceActionTestKit(new SomeServiceAction(_))
      pending
      // use the testkit to execute a command
      // and verify final updated state:
      // val result = service.someOperation(SomeRequest)
      // verify the reply
      // result.reply shouldBe expectedReply
    }

    "handle command simpleMethod" in {
      val service = SomeServiceActionTestKit(new SomeServiceAction(_))
          pending
      // val result = service.simpleMethod(SomeRequest(...))
    }

  }
}
