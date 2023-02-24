package com.example.json

import kalix.scalasdk.JsonSupport
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
      result.reply shouldBe Empty.defaultInstance
    }

    "handle command Produce" in {
      val testKit = MyServiceActionTestKit(new MyServiceAction(_))
      val result = testKit.produce(KeyValue("key", 6))
      val decoded = JsonSupport.decodeJson[KeyValue](result.reply)
      decoded shouldBe KeyValue("key", 6)
    }

  }
}
