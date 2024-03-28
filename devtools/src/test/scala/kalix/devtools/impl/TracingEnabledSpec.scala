package kalix.devtools.impl

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TracingEnabledSpec  extends AnyWordSpec with Matchers {

  "TracingEnabled util" should {

    "extract if tracing is enabled" in {
      val line = {
        "-Dsomething.else=10 " +
        "-Dkalix.proxy.telemetry.tracing.enabled=true " +
        "-Dkalix.proxy.telemetry.tracing.collector-endpoint=\"http://jaeger:4317\""
      }

      TracingConfExtractor.unapply(line) shouldBe Some(TracingConfig(enabled = true, "http://jaeger:4317"))

    }
  }

}
