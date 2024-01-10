/*
 * Copyright 2024 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
