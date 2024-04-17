/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.devtools.impl

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TracingPortExtractorSpec extends AnyWordSpec with Matchers {

  "TracingEnabled util" should {

    "extract if tracing is enabled" in {
      val lines: Seq[String] = Seq(
        "-Dsomething.else=10 ",
        "-Dkalix.proxy.telemetry.tracing.enabled=true ",
        "-Dkalix.proxy.telemetry.tracing.collector-endpoint=http://jaeger:4317")

      TracingPortExtractor.unapply(lines) shouldBe Some(4317)
    }

    val defaultFile =
      """
        |version: "3"
        |services:
        |  kalix-runtime:
        |    image: gcr.io/kalix-public/kalix-runtime:1.1.34
        |    container_name: tracing
        |    ports:
        |      - "9000:9000"
        |    extra_hosts:
        |      - "host.docker.internal:host-gateway"
        |    environment:
        |      JAVA_TOOL_OPTIONS: >
        |        -Dmyproperty=false -Dkalix.proxy.telemetry.tracing.collector-endpoint=http://jaeger:4317
        |        -Dkalix.proxy.telemetry.tracing.enabled=true
        |      USER_SERVICE_HOST: localhost
        |      USER_SERVICE_PORT: 8080
        |      # Comment to enable ACL check in dev-mode (see https://docs.kalix.io/services/using-acls.html#_local_development_with_acls)
        |      ACL_ENABLED: "false"
        |  jaeger:
        |    image: jaegertracing/all-in-one:1.54
        |    ports:
        |      - 4317:4317
        |      - 16686:16686
        |""".stripMargin

    "extract if tracing is enabled in different order and with multiple params in the same line" in {
      TracingPortExtractor.unapply(defaultFile.linesIterator.toSeq) shouldBe Some(4317)
    }

    "extract none if tracing is enabled, but port is not set" in {
      val lines: Seq[String] = Seq("-Dsomething.else=10 ", "-Dkalix.proxy.telemetry.tracing.enabled=true")

      TracingPortExtractor.unapply(lines) shouldBe None
    }

    "extract none if tracing is enable, but port is invalid (negative)" in {
      val lines: Seq[String] = Seq(
        "-Dsomething.else=10 ",
        "-Dkalix.proxy.telemetry.tracing.enabled=true",
        "-Dkalix.proxy.telemetry.tracing.collector-endpoint=http://jaeger:-1233")

      TracingPortExtractor.unapply(lines) shouldBe None
    }

    "extract none if tracing is enable, but port out of range" in {
      val lines: Seq[String] = Seq(
        "-Dsomething.else=10 ",
        "-Dkalix.proxy.telemetry.tracing.enabled=true",
        "-Dkalix.proxy.telemetry.tracing.collector-endpoint=http://jaeger:3")

      TracingPortExtractor.unapply(lines) shouldBe None
    }

  }

}
