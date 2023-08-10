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

package kalix.javasdk.impl.action

import akka.NotUsed
import akka.stream.javadsl.Source
import com.google.protobuf.any.{ Any => ScalaPbAny }
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.exporter.logging.LoggingSpanExporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.`export`.SimpleSpanProcessor
import kalix.javasdk.Metadata
import kalix.javasdk.action.Action
import kalix.javasdk.action.MessageEnvelope
import kalix.javasdk.impl.AnySupport.ProtobufEmptyTypeUrl
import kalix.javasdk.impl.CommandHandler
import kalix.javasdk.impl.InvocationContext
import kalix.javasdk.impl.action.ReflectiveActionRouter.openTelemetry
import kalix.spring.impl.KalixSpringApplication
import org.slf4j.LoggerFactory

// TODO: abstract away reactor dependency
import com.google.protobuf.{ ByteString => ProtobufByteString }
import reactor.core.publisher.Flux

class ReflectiveActionRouter[A <: Action](
    action: A,
    commandHandlers: Map[String, CommandHandler],
    ignoreUnknown: Boolean)
    extends ActionRouter[A](action) {

  val logger = LoggerFactory.getLogger(this.getClass)

  private def commandHandlerLookup(commandName: String) =
    commandHandlers.getOrElse(commandName, throw new RuntimeException(s"no matching method for '$commandName'"))

  override def handleUnary(commandName: String, message: MessageEnvelope[Any]): Action.Effect[_] = {

    val commandHandler = commandHandlerLookup(commandName)

    val invocationContext =
      InvocationContext(
        message.payload().asInstanceOf[ScalaPbAny],
        commandHandler.requestMessageDescriptor,
        message.metadata())

    val inputTypeUrl = message.payload().asInstanceOf[ScalaPbAny].typeUrl
    val methodInvoker = commandHandler.lookupInvoker(inputTypeUrl)

    import io.opentelemetry.context.{ Context => OtelContext }

    import scala.jdk.OptionConverters._

    val getter = new TextMapGetter[Metadata]() {
      override def get(carrier: Metadata, key: String): String = {
        logger.debug("For the key [{}] the value is [{}]", key, carrier.get(key))
        carrier.get(key).toScala.getOrElse("")
      }

      override def keys(carrier: Metadata): java.lang.Iterable[String] =
        carrier.getAllKeys
    }

    val context = KalixSpringApplication.openTelemetry.getPropagators.getTextMapPropagator
      .extract(OtelContext.current(), message.metadata(), getter.asInstanceOf[TextMapGetter[Object]])
    val tracer = KalixSpringApplication.openTelemetry.getTracer("kalix.javasdk.impl.action.ReflectiveActionRouter")

    val span = tracer.spanBuilder(commandName).setParent(context).setSpanKind(SpanKind.SERVER).startSpan()
    val scope = span.makeCurrent() //TODO use Using for closeable?
    try {
      span.setAttribute("action", "testName" + commandName)
      methodInvoker match {
        case Some(invoker) =>
          inputTypeUrl match {
            case ProtobufEmptyTypeUrl =>
              invoker
                .invoke(action)
                .asInstanceOf[Action.Effect[_]]
            case _ =>
              invoker
                .invoke(action, invocationContext)
                .asInstanceOf[Action.Effect[_]]
          }
        case None if ignoreUnknown => ActionEffectImpl.Builder.ignore()
        case None =>
          throw new NoSuchElementException(
            s"Couldn't find any method with input type [$inputTypeUrl] in Action [$action].")

      }
    } finally {
      span.end()
      scope.close()
    }
  }

  override def handleStreamedOut(
      commandName: String,
      message: MessageEnvelope[Any]): Source[Action.Effect[_], NotUsed] = {

    val componentMethod = commandHandlerLookup(commandName)

    val context =
      InvocationContext(
        message.payload().asInstanceOf[ScalaPbAny],
        componentMethod.requestMessageDescriptor,
        message.metadata())

    val inputTypeUrl = message.payload().asInstanceOf[ScalaPbAny].typeUrl

    componentMethod.lookupInvoker(inputTypeUrl) match {
      case Some(methodInvoker) =>
        val response = methodInvoker.invoke(action, context).asInstanceOf[Flux[Action.Effect[_]]]
        Source.fromPublisher(response)
      case None if ignoreUnknown => Source.empty()
      case None =>
        throw new NoSuchElementException(
          s"Couldn't find any method with input type [$inputTypeUrl] in Action [$action].")
    }
  }

  override def handleStreamedIn(commandName: String, stream: Source[MessageEnvelope[Any], NotUsed]): Action.Effect[_] =
    throw new IllegalArgumentException("Stream in calls are not supported")

  // TODO: to implement
  override def handleStreamed(
      commandName: String,
      stream: Source[MessageEnvelope[Any], NotUsed]): Source[Action.Effect[_], NotUsed] =
    throw new IllegalArgumentException("Stream in calls are not supported")
}
