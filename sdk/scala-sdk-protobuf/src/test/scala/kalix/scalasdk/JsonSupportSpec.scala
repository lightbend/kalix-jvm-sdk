/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

/*
 * Copyright (C) 2009-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package kalix.scalasdk

import akka.Done
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import kalix.javasdk.{ JsonSupport => JavaJsonSupport }
import com.fasterxml.jackson.annotation.JsonCreator

@JsonCreator
case class MyJsonable(field: String)

class JsonSupportSpec extends AnyWordSpec with Matchers {

  val myJsonable = MyJsonable("foo")

  "JsonSupport" must {

    "serialize and deserialize JSON" in {
      val any = JsonSupport.encodeJson(myJsonable)
      any.typeUrl should ===(JavaJsonSupport.KALIX_JSON + classOf[MyJsonable].getName)
      JsonSupport.decodeJson[MyJsonable](any).field should ===("foo")
    }

    "serialize and deserialize Akka Done class" in {
      val done = Done
      val any = JsonSupport.encodeJson(done)
      any.typeUrl should ===(JavaJsonSupport.KALIX_JSON + Done.getClass.getName)
      JsonSupport.decodeJson[Done](any) shouldBe Done
    }

    "serialize JSON with an explicit type url suffix" in {
      val any = JsonSupport.encodeJson(myJsonable, "bar")
      any.typeUrl should ===(JavaJsonSupport.KALIX_JSON + "bar")
    }

    "conditionally decode JSON depending on suffix" in {
      val any = JsonSupport.encodeJson(myJsonable, "bar")
      any.typeUrl should ===(JavaJsonSupport.KALIX_JSON + "bar")
      JsonSupport.decodeJson[MyJsonable]("other", any).isDefined should ===(false)
      val decoded = JsonSupport.decodeJson[MyJsonable]("bar", any)
      decoded should ===(Some(myJsonable))
    }
  }
}
