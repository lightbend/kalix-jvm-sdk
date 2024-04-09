/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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
