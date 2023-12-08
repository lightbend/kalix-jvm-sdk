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

package kalix.javasdk.impl.telemetry

import akka.actor.ActorSystem
import akka.actor.ExtendedActorSystem
import akka.actor.Extension
import akka.actor.ExtensionId
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
import kalix.javasdk.Metadata
import kalix.javasdk.impl.MetadataImpl
import kalix.javasdk.impl.ProxyInfoHolder
import kalix.javasdk.impl.Service
import kalix.protocol.action.ActionCommand
import kalix.protocol.entity.Command
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.jdk.OptionConverters._

object Telemetry extends ExtensionId[Telemetry] {

  override def createExtension(system: ExtendedActorSystem): Telemetry =
    new Telemetry(system)
}

sealed trait ComponentCategory {
  def name: String
}
case object ActionCategory extends ComponentCategory {
  def name = "Action"
}
case object EventSourcedEntityCategory extends ComponentCategory {
  def name = "Event Sourced Entity"
}

case object ValueEntityCategory extends ComponentCategory {
  def name = "Value Entity"
}

final class Telemetry(system: ActorSystem) extends Extension {

  private val proxyInfoHolder = ProxyInfoHolder(system)

  private val logger = LoggerFactory.getLogger(classOf[Telemetry])

  private val collectorEndpointSDK = system.settings.config.getString(TraceInstrumentation.TRACING_ENDPOINT)

  implicit val ec = ExecutionContext.Implicits.global

  /**
   * This method assumes the instrumentation won't be consumed until discovery from the proxy is requested. Therefore
   * this should be stored in a `lazy` value and only used after we are sure the ProxyInfo has been process. For
   * example, in an Action inside the methods `handleyUnary` and alike.
   * @param componentName
   * @param componentCategory
   * @return
   */
  def traceInstrumentation(componentName: String, componentCategory: ComponentCategory): Instrumentation = {
    val collectorEndpoint = {
      if (collectorEndpointSDK.nonEmpty) collectorEndpointSDK
      else
        proxyInfoHolder.proxyTracingCollectorEndpoint match {
          case Some(endpoint) => endpoint
          case None => throw new IllegalArgumentException("Tracing endpoint from the Proxy not yet received. Retry.")
        }
    }
    logger.debug("collectorEndpointSDK [{}].", collectorEndpointSDK)
    if (collectorEndpoint.isEmpty) {
      logger.debug("Instrumentation disabled. Set to NoOp.")
      NoOpInstrumentation
    } else {
      logger.debug("Instrumentation enabled. Set collector endpoint to [{}].", collectorEndpoint)
      new TraceInstrumentation(collectorEndpoint, componentName, system, componentCategory)
    }
  }
}

trait Instrumentation {

  //private now?

  def buildSpan(service: Service, command: Command): Option[Span]
  //private now?

  def buildSpan(service: Service, command: ActionCommand): Option[Span]

}

private object TraceInstrumentation {

  val TRACE_PARENT_KEY = "traceparent"
  val TRACING_ENDPOINT = "kalix.telemetry.tracing.collector-endpoint"

  val logger: Logger = LoggerFactory.getLogger(getClass)

  lazy val otelGetter = new TextMapGetter[Metadata]() {
    override def get(carrier: Metadata, key: String): String = {
      logger.debug("For the key [{}] the value is [{}]", key, carrier.get(key))
      carrier.get(key).toScala.getOrElse("")
    }

    override def keys(carrier: Metadata): java.lang.Iterable[String] =
      carrier.getAllKeys
  }

}

private final class TraceInstrumentation(
    collectorEndpoint: String,
    componentName: String,
    system: ActorSystem,
    componentCategory: ComponentCategory)
    extends Instrumentation {

  import TraceInstrumentation._

  val tracePrefix = componentCategory.name

  private val openTelemetry: OpenTelemetry = {
    val resource =
      Resource.getDefault.merge(
        Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, tracePrefix + " : " + componentName)))
    val sdkTracerProvider = SdkTracerProvider
      .builder()
      .addSpanProcessor(
        SimpleSpanProcessor.create(
          OtlpGrpcSpanExporter
            .builder()
            .setEndpoint(collectorEndpoint)
            .build()))
      .setResource(resource)
      .build()

    val sdk = OpenTelemetrySdk
      .builder()
      .setTracerProvider(sdkTracerProvider)
      .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
      .build()
    system.registerOnTermination(sdk.close())
    sdk
  }

  /**
   * Creates a span if it finds a trace parent in the command's metadata
   * @param service
   * @param command
   * @return
   */
  override def buildSpan(service: Service, command: Command): Option[Span] = {
    if (logger.isTraceEnabled) logger.trace("Building span for command [{}].", command)
    val metadata = new MetadataImpl(command.metadata.map(_.entries).getOrElse(Nil))
    if (metadata.get(TRACE_PARENT_KEY).isPresent) {
      if (logger.isTraceEnabled) logger.trace("`traceparent` found")

      val context = openTelemetry.getPropagators.getTextMapPropagator
        .extract(OtelContext.current(), metadata, otelGetter.asInstanceOf[TextMapGetter[Object]])

      val span = openTelemetry
        .getTracer("java-sdk")
        .spanBuilder(s"""${command.entityId}""")
        .setParent(context)
        .setSpanKind(SpanKind.SERVER)
        .startSpan()
      Some(
        span
          .setAttribute("service.name", s"""${service.serviceName}.${command.entityId}""")
          .setAttribute("component.type", service.componentType)
          .setAttribute("entity.id", command.entityId))
    } else {
      if (logger.isTraceEnabled) logger.trace("No `traceparent` found for command [{}].", command)
      None
    }
  }

  override def buildSpan(service: Service, command: ActionCommand): Option[Span] = {
    if (logger.isTraceEnabled) logger.trace("Building span for action command [{}].", command)

    val metadata = new MetadataImpl(command.metadata.map(_.entries).getOrElse(Nil))
    if (metadata.get(TRACE_PARENT_KEY).isPresent) {
      if (logger.isTraceEnabled) logger.trace("`traceparent` found")

      val context = openTelemetry.getPropagators.getTextMapPropagator
        .extract(OtelContext.current(), metadata, otelGetter.asInstanceOf[TextMapGetter[Object]])

      val span = openTelemetry
        .getTracer("java-sdk")
        .spanBuilder(s"""${command.name}""")
        .setParent(context)
        .setSpanKind(SpanKind.SERVER)
        .startSpan()
      Some(
        span
          .setAttribute("service.name", s"""${service.serviceName}""")
          .setAttribute(s"${service.componentType}", command.name))
    } else {
      if (logger.isTraceEnabled) logger.trace("No `traceparent` found for command [{}].", command)
      None
    }
  }
}

private object NoOpInstrumentation extends Instrumentation {

  override def buildSpan(service: Service, command: Command): Option[Span] = None

  override def buildSpan(service: Service, command: ActionCommand): Option[Span] = None
}
