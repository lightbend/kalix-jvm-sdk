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

package kalix.javasdk.impl

import akka.http.scaladsl.model.HttpMethods
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.{ Context => OtelContext }
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.`export`.SimpleSpanProcessor
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes
import kalix.javasdk.Metadata
import kalix.javasdk.action.Action
import kalix.javasdk.eventsourcedentity.EventSourcedEntity
import kalix.javasdk.valueentity.ValueEntity
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.reflect.Method
import scala.jdk.OptionConverters._

object Telemetry {

  val TRACE_PARENT_KEY = "traceparent"

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  val otelGetter = new TextMapGetter[Metadata]() {
    override def get(carrier: Metadata, key: String): String = {
      logger.debug("For the key [{}] the value is [{}]", key, carrier.get(key))
      carrier.get(key).toScala.getOrElse("")
    }

    override def keys(carrier: Metadata): java.lang.Iterable[String] =
      carrier.getAllKeys
  }
}

class Telemetry(serviceName: String) {

  val openTelemetry: OpenTelemetry = {

    val resource =
      Resource.getDefault.merge(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, serviceName)))
    val sdkTracerProvider =
      SdkTracerProvider
        .builder()
        .addSpanProcessor(
          SimpleSpanProcessor.create(OtlpGrpcSpanExporter.builder().setEndpoint("http://jaeger:4317").build()))
        .setResource(resource)
        .build()
    val sdk = OpenTelemetrySdk
      .builder()
      .setTracerProvider(sdkTracerProvider)
      .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
      .build()
    //    Runtime.getRuntime().addShutdownHook(new Thread(sdkTracerProvider.close)) TODO add shutdown
    sdk
  }

  /**
   * Builds the span by extracting the traceparent from the `metadata` and starts the span
   *
   * @param tracerName
   * @param spanName
   * @param metadata
   * @return
   */
  def buildSpan[T](
      component: T,
      inputTypeUrl: String,
      methodInvoker: Option[MethodInvoker],
      metadata: Metadata): Option[Span] = {
    //TODO find if we have this mapping already
    def getComponentName[T](componentType: T): String =
      componentType match {
        case _: Action                   => "action"
        case _: EventSourcedEntity[_, _] => "event-sourced-entity"
        case _: ValueEntity[_]           => "value-entity"
      }

    def getHttpMethod(method: Method): String = {
      val httpMethods = method.getDeclaredAnnotations.map { ann =>
        ann match {
          case _: org.springframework.web.bind.annotation.PostMapping   => Some(HttpMethods.POST)
          case _: org.springframework.web.bind.annotation.GetMapping    => Some(HttpMethods.GET)
          case _: org.springframework.web.bind.annotation.PatchMapping  => Some(HttpMethods.PATCH)
          case _: org.springframework.web.bind.annotation.PutMapping    => Some(HttpMethods.PUT)
          case _: org.springframework.web.bind.annotation.DeleteMapping => Some(HttpMethods.DELETE)
          case _                                                        => None
        }
      }
      httpMethods.flatten.headOption match {
        case Some(httpMethod) => httpMethod.value
        case None             => "No @RequestMapping annotation"
      }
    }

    if (metadata.get(Telemetry.TRACE_PARENT_KEY).isPresent && methodInvoker.isDefined) {
      val method = methodInvoker.get.method

      val context = openTelemetry.getPropagators.getTextMapPropagator
        .extract(OtelContext.current(), metadata, Telemetry.otelGetter.asInstanceOf[TextMapGetter[Object]])
      val tracer = openTelemetry.getTracer("java-sdk-spring")
      val span = tracer
        .spanBuilder(s"""${component.getClass.getName}.${method.getName}""")
        .setParent(context)
        .setSpanKind(SpanKind.SERVER)
        .startSpan()
      Some(
        span
          .setAttribute(SemanticAttributes.HTTP_METHOD, getHttpMethod(method))
          .setAttribute("server.address", "localhost") // TODO use Kubernetes .svc name?
          .setAttribute("server.port", 8080) // TODO read the port from properties
          .setAttribute("server.scheme", "http")
          .setAttribute("service.name", method.getName)
          .setAttribute("kalix.inputType", inputTypeUrl)
          //          .setAttribute("kalix.input", methodInvoker.get.method.input.toString)
          .setAttribute("kalix." + getComponentName(component), component.getClass.getName))
    } else None

  }

}
