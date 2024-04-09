/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.devtools.impl

import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ServicePortMappingsExtractorSpec extends AnyWordSpec with Matchers with OptionValues {

  "ServicePortMappingsExtractor" should {

    "collect all occurrences of kalix.dev-mode.service-port-mappings" in {
      val line =
        "-Dsomething.else=10 " +
        "-Dkalix.dev-mode.service-port-mappings.foo=9001 " +
        "-Dsomething.in.the.middle=20 " +
        "-Dkalix.dev-mode.service-port-mappings.bar=9002 " +
        "-Dkalix.dev-mode.service-port-mappings.baz=9003" +
        "-Dkalix.dev-mode.service-port-mappings.qux=somehost:9004"

      ServicePortMappingsExtractor.unapply(line).value shouldBe Seq(
        "foo=9001",
        "bar=9002",
        "baz=9003",
        "qux=somehost:9004")
    }

    "collect nothing if there are no occurrences of kalix.dev-mode.service-port-mappings" in {
      val line =
        "-Dsomething.else=10 " +
        "-Dsomething.in.the.middle=20"

      ServicePortMappingsExtractor.unapply(line) shouldBe None
    }
  }
}
