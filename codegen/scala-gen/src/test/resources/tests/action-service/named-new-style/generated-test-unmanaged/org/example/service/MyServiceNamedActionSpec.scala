package org.example.service

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.google.protobuf.empty.Empty
import kalix.scalasdk.action.Action
import kalix.scalasdk.testkit.ActionResult
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class MyServiceNamedActionSpec
    extends AnyWordSpec
    with Matchers {

  "MyServiceNamedAction" must {

    "have example test that can be removed" in {
      val service = MyServiceNamedActionTestKit(new MyServiceNamedAction(_))
      pending
      // use the testkit to execute a command
      // and verify final updated state:
      // val result = service.someOperation(SomeRequest)
      // verify the reply
      // result.reply shouldBe expectedReply
    }

    "handle command simpleMethod" in {
      val service = MyServiceNamedActionTestKit(new MyServiceNamedAction(_))
          pending
      // val result = service.simpleMethod(MyRequest(...))
    }

    "handle command streamedOutputMethod" in {
      val service = MyServiceNamedActionTestKit(new MyServiceNamedAction(_))
          pending
      // val result = service.streamedOutputMethod(MyRequest(...))
    }

    "handle command streamedInputMethod" in {
      val service = MyServiceNamedActionTestKit(new MyServiceNamedAction(_))
          pending
      // val result = service.streamedInputMethod(Source.single(MyRequest(...)))
    }

    "handle command fullStreamedMethod" in {
      val service = MyServiceNamedActionTestKit(new MyServiceNamedAction(_))
          pending
      // val result = service.fullStreamedMethod(Source.single(MyRequest(...)))
    }

  }
}
