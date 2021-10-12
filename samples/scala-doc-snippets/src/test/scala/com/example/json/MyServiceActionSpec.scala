package com.example.json

import com.akkaserverless.scalasdk.JsonSupport
import com.akkaserverless.scalasdk.action.Action
import com.akkaserverless.scalasdk.testkit.ActionResult
import com.google.protobuf.ByteString
import com.google.protobuf.any.{Any => ScalaPbAny}
import com.google.protobuf.empty.Empty
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MyServiceActionSpec
    extends AnyWordSpec
    with Matchers {

  "MyServiceAction" must {

    "handle command Consume" in {
      val testKit = MyServiceActionTestKit(new MyServiceAction(_))
      val result = testKit.consume(JsonSupport.encodeJson(KeyValue("key", 5)))
    }

    "handle command Produce" in {
      val testKit = MyServiceActionTestKit(new MyServiceAction(_))
      val result = testKit.produce(KeyValue("key", 6))
      val decoded = JsonSupport.decodeJson[KeyValue](result.reply)
      decoded shouldBe KeyValue("key", 6)
    }

  }
}
