/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.testkit

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.util.concurrent.atomic.AtomicInteger

class KalixTestKitRetrySpec extends AnyWordSpec with Matchers {

  "KalixTestKit.start" should {

    "fail with RuntimeException after exhausting all 3 attempts" in {
      val attempts = new AtomicInteger(0)

      val kit = new KalixTestKit(
        _ => {
          attempts.incrementAndGet()
          throw new RuntimeException("simulated failure")
        },
        KalixTestKit.Settings.DEFAULT)

      val ex = intercept[RuntimeException] {
        kit.start()
      }

      ex.getMessage should include("3 attempts")
      attempts.get() shouldBe 3
    }

    "succeed on the second attempt" in {
      val attempts = new AtomicInteger(0)

      val kit = new KalixTestKit(
        _ => {
          if (attempts.incrementAndGet() < 2)
            throw new RuntimeException("simulated failure")
          // second attempt succeeds
        },
        KalixTestKit.Settings.DEFAULT)

      kit.start() // should not throw
      attempts.get() shouldBe 2
    }
  }
}
