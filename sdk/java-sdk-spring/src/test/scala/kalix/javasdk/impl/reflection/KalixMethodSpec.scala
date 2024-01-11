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

package kalix.javasdk.impl.reflection

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class KalixMethodSpec extends AnyWordSpec with Matchers {

  "A KalixMethod" should {
    "merge eventing out with in doesn't remove in" in {
      val eventingWithIn = kalix.Eventing.newBuilder().setIn(kalix.EventSource.newBuilder().setTopic("a"))
      val eventingWithOut = kalix.Eventing.newBuilder().setOut(kalix.EventDestination.newBuilder().setTopic("b"))

      val original = kalix.MethodOptions.newBuilder().setEventing(eventingWithIn)
      val addOn = kalix.MethodOptions.newBuilder().setEventing(eventingWithOut)
      val kalixMethod = KalixMethod(VirtualServiceMethod(classOf[Integer], "", classOf[Integer]))
        .mergeKalixOptions(Some(original.build()), addOn.build())

      kalixMethod.getEventing.getIn.getTopic shouldBe "a"
      kalixMethod.getEventing.getOut.getTopic shouldBe "b"
    }
  }
}
