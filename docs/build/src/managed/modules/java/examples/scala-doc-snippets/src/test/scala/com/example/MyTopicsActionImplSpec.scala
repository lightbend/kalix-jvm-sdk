package com.example

import com.google.protobuf.empty.Empty
import com.google.protobuf.wrappers.BytesValue
import com.google.protobuf.wrappers.StringValue
import kalix.scalasdk.action.Action
import kalix.scalasdk.testkit.ActionResult
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class MyTopicsActionImplSpec
    extends AnyWordSpec
    with Matchers {

  "MyTopicsActionImpl" must {

    "have example test that can be removed" in {
      val service = MyTopicsActionImplTestKit(new MyTopicsActionImpl(_))
      pending
      // use the testkit to execute a command
      // and verify final updated state:
      // val result = service.someOperation(SomeRequest)
      // verify the reply
      // result.reply shouldBe expectedReply
    }

    "handle command ConsumeStringTopic" in {
      val service = MyTopicsActionImplTestKit(new MyTopicsActionImpl(_))
          pending
      // val result = service.consumeStringTopic(StringValue(...))
    }

    "handle command ConsumeRawBytesTopic" in {
      val service = MyTopicsActionImplTestKit(new MyTopicsActionImpl(_))
          pending
      // val result = service.consumeRawBytesTopic(BytesValue(...))
    }

    "handle command ProtobufFromTopic" in {
      val service = MyTopicsActionImplTestKit(new MyTopicsActionImpl(_))
          pending
      // val result = service.protobufFromTopic(TopicOperation(...))
    }

  }
}
