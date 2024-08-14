/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.testkit.telemetry

import kalix.javasdk.impl.telemetry.Telemetry
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class TelemetrySpec extends AnyFlatSpec with should.Matchers {

  "Telemetry" should "be able to find the traceId in a traceParent" in {
    val traceparent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"

    Telemetry.extractTraceId(traceparent) shouldBe "4bf92f3577b34da6a3ce929d0e0e4736"
  }

  "Telemetry" should "return empty if no traceId is found" in {
    val traceparent = "0-4bf92f3577b34da6d0e0e4736-00fa0ba902b7-1"

    Telemetry.extractTraceId(traceparent) shouldBe ""
  }

}
