/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.testkit.telemetry

import kalix.javasdk.Metadata
import kalix.javasdk.impl.MetadataImpl
import kalix.javasdk.impl.telemetry.TraceInstrumentation
import kalix.protocol.component.MetadataEntry
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class TraceInstrumentationSpec extends AnyFlatSpec with should.Matchers {

  "TraceInstrumentation" should "be able to find the traceId in a traceParent" in {
    val metadata = MetadataImpl.of(
      Seq(
        MetadataEntry(
          "traceparent",
          MetadataEntry.Value.StringValue("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"))))

    TraceInstrumentation.extractTraceId(metadata) shouldBe "4bf92f3577b34da6a3ce929d0e0e4736"
  }

  "TraceInstrumentation" should "return empty if no traceId is found" in {
    TraceInstrumentation.extractTraceId(
      Metadata.EMPTY) shouldBe "00000000000000000000000000000000" //see INVALID in TraceId https://github.com/open-telemetry/opentelemetry-java/blob/ad120a5bff0887dffedb9c73af8e8e0aeb63659a/api/all/src/main/java/io/opentelemetry/api/trace/TraceId.java#L32
  }

}
