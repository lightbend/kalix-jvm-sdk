/*
 * Copyright 2021 Lightbend Inc.
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

package kalix.javasdk

import akka.Done
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.beans.BeanProperty

class MyJsonable {
  @BeanProperty var field: String = _
}

class JsonSupportSpec extends AnyWordSpec with Matchers {

  val myJsonable = new MyJsonable
  myJsonable.field = "foo"

  "JsonSupport" must {
    "serialize and deserialize JSON" in {
      val any = JsonSupport.encodeJson(myJsonable)
      any.getTypeUrl should ===(JsonSupport.KALIX_JSON + classOf[MyJsonable].getName)
      JsonSupport.decodeJson(classOf[MyJsonable], any).field should ===("foo")
    }
    "serialize and deserialize Akka Done class" in {
      val done = Done.getInstance()
      val any = JsonSupport.encodeJson(done)
      any.getTypeUrl should ===(JsonSupport.KALIX_JSON + Done.getClass.getName)
      JsonSupport.decodeJson(classOf[Done], any) shouldBe Done.getInstance()
    }
    "serialize JSON with an explicit type url suffix" in {
      val any = JsonSupport.encodeJson(myJsonable, "bar")
      any.getTypeUrl should ===(JsonSupport.KALIX_JSON + "bar")
    }
    "conditionally decode JSON depending on suffix" in {
      val any = JsonSupport.encodeJson(myJsonable, "bar")
      any.getTypeUrl should ===(JsonSupport.KALIX_JSON + "bar")
      JsonSupport.decodeJson(classOf[MyJsonable], "other", any).isPresent() should ===(false)
      val decoded = JsonSupport.decodeJson(classOf[MyJsonable], "bar", any)
      decoded.isPresent() should ===(true)
      decoded.get().field should ===("foo")
    }
  }

}
