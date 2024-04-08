/*
 * Copyright 2024 Lightbend Inc.
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
import akka.actor.ActorSystem
import akka.stream.scaladsl.{ Sink, Source }
import com.google.protobuf.Descriptors
import com.google.protobuf.any.Any
import io.grpc.Status
import io.opentelemetry.api.trace.{ Span, SpanContext, Tracer }
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import kalix.javasdk._
import kalix.javasdk.action._
import kalix.javasdk.impl.ErrorHandling.BadRequestException
import kalix.javasdk.impl._
import kalix.javasdk.impl.effect.EffectSupport.asProtocol
import kalix.javasdk.impl.telemetry.TraceInstrumentation.{ TRACE_PARENT_KEY, TRACE_STATE_KEY }
import kalix.javasdk.impl.telemetry.{ ActionCategory, Instrumentation, Telemetry, TraceInstrumentation }
import kalix.protocol.action.{ ActionCommand, ActionResponse, Actions }
import kalix.protocol.component
import kalix.protocol.component.{ Failure, MetadataEntry }
import org.slf4j.{ Logger, LoggerFactory }

import java.util.Optional
import scala.compat.java8.OptionConverters.RichOptionForJava8
import scala.concurrent.Future
import scala.jdk.CollectionConverters.SeqHasAsJava
import scala.util.control.NonFatal

final class ActionService(
    val factory: ActionFactory,
    override val descriptor: Descriptors.ServiceDescriptor,
    override val additionalDescriptors: Array[Descriptors.FileDescriptor],
    val messageCodec: MessageCodec,
    val actionOptions: Option[ActionOptions])
    extends Service {

  def this(
      factory: ActionFactory,
      descriptor: Descriptors.ServiceDescriptor,
      additionalDescriptors: Array[Descriptors.FileDescriptor],
      messageCodec: MessageCodec,
      actionOptions: ActionOptions) =
    this(factory, descriptor, additionalDescriptors, messageCodec, Some(actionOptions))

  @volatile var actionClass: Option[Class[_]] = None

  def createAction(context: ActionCreationContext): ActionRouter[_] = {
    val handler = factory.create(context)
    actionClass = Some(handler.actionClass())
    handler
  }

  // use a logger specific to the service impl if possible (concrete action was successfully created at least once)
  def log: Logger = actionClass match {
    case Some(clazz) => LoggerFactory.getLogger(clazz)
    case None        => ActionsImpl.log
  }

  override def resolvedMethods: Option[Map[String, ResolvedServiceMethod[_, _]]] =
    factory match {
      case resolved: ResolvedEntityFactory => Some(resolved.resolvedMethods)
      case _                               => None
    }

  override def componentOptions: Option[ComponentOptions] = actionOptions

  override final val componentType = Actions.name
}

private[javasdk] object ActionsImpl {
  private[action] val log = LoggerFactory.getLogger(classOf[ActionsImpl])

  private def handleUnexpectedException(
      service: ActionService,
      command: ActionCommand,
      ex: Throwable): ActionResponse = {
    ex match {
      case badReqEx: BadRequestException => handleBadRequest(badReqEx.getMessage)
      case _ =>
        ErrorHandling.withCorrelationId { correlationId =>
          service.log.error(s"Failure during handling of command ${command.serviceName}.${command.name}", ex)
          protocolFailure(correlationId)
        }
    }
  }

  private def handleBadRequest(description: String): ActionResponse =
    ActionResponse(ActionResponse.Response.Failure(Failure(0, description, Status.Code.INVALID_ARGUMENT.value())))

  private def protocolFailure(correlationId: String): ActionResponse = {
    ActionResponse(ActionResponse.Response.Failure(Failure(0, s"Unexpected error [$correlationId]")))
  }

}

private[javasdk] final class ActionsImpl(
    _system: ActorSystem,
    services: Map[String, ActionService],
    rootContext: Context)
    extends Actions {

  import ActionsImpl._
  import _system.dispatcher
  implicit val system: ActorSystem = _system
  private val telemetry = Telemetry(system)
  lazy val telemetries: Map[String, Instrumentation] = services.values.map { s =>
    (s.serviceName, telemetry.traceInstrumentation(s.serviceName, ActionCategory))
  }.toMap

  private def effectToResponse(
      service: ActionService,
      command: ActionCommand,
      effect: Action.Effect[_],
      messageCodec: MessageCodec): Future[ActionResponse] = {
    import ActionEffectImpl._
    effect match {
      case ReplyEffect(message, metadata, sideEffects) =>
        val response =
          component.Reply(Some(messageCodec.encodeScala(message)), metadata.flatMap(MetadataImpl.toProtocol))
        Future.successful(
          ActionResponse(ActionResponse.Response.Reply(response), toProtocol(messageCodec, sideEffects)))
      case ForwardEffect(forward: GrpcDeferredCall[_, _], sideEffects) =>
        val response = component.Forward(
          forward.fullServiceName,
          forward.methodName,
          Some(messageCodec.encodeScala(forward.message)),
          MetadataImpl.toProtocol(forward.metadata))
        Future.successful(
          ActionResponse(ActionResponse.Response.Forward(response), toProtocol(messageCodec, sideEffects)))
      case ForwardEffect(forward: RestDeferredCall[Any @unchecked, _], sideEffects) =>
        val response = component.Forward(
          forward.fullServiceName,
          forward.methodName,
          Some(forward.message),
          MetadataImpl.toProtocol(forward.metadata))
        Future.successful(
          ActionResponse(ActionResponse.Response.Forward(response), toProtocol(messageCodec, sideEffects)))
      case AsyncEffect(futureEffect, sideEffects) =>
        futureEffect
          .flatMap { effect =>
            val withSurroundingSideEffects =
              if (sideEffects.isEmpty) effect
              else if (!effect.canHaveSideEffects) {
                log.warn(
                  "Side effects added to asyncEffect, but the inner effect [{}] does not support side effects, side effects dropped",
                  effect.getClass.getName)
                effect
              } else effect.addSideEffects(sideEffects.asJava)
            effectToResponse(service, command, withSurroundingSideEffects, messageCodec)
          }
          .recover { case NonFatal(ex) =>
            handleUnexpectedException(service, command, ex)
          }
      case ErrorEffect(description, status, sideEffects) =>
        Future.successful(
          ActionResponse(
            ActionResponse.Response.Failure(
              Failure(description = description, grpcStatusCode = status.map(_.value()).getOrElse(0))),
            toProtocol(messageCodec, sideEffects)))
      case IgnoreEffect =>
        Future.successful(ActionResponse(ActionResponse.Response.Empty, toProtocol(messageCodec, Nil)))
      case unknown =>
        throw new IllegalArgumentException(s"Unknown Action.Effect type ${unknown.getClass}")
    }
  }

  private def toProtocol(messageCodec: MessageCodec, sideEffects: Seq[SideEffect]): Seq[component.SideEffect] =
    sideEffects.map(asProtocol(messageCodec, _))

  /**
   * Handle a unary command. The input command will contain the service name, command name, request metadata and the
   * command payload. The reply may contain a direct reply, a forward or a failure, and it may contain many side
   * effects.
   */
  override def handleUnary(in: ActionCommand): Future[ActionResponse] =
    services.get(in.serviceName) match {
      case Some(service) =>
        val span = telemetries(service.serviceName).buildSpan(service, in)

        val fut =
          try {
            val context = createContext(in, service.messageCodec, span.map(_.getSpanContext), service.serviceName)
            val decodedPayload = service.messageCodec.decodeMessage(
              in.payload.getOrElse(throw new IllegalArgumentException("No command payload")))
            val effect = service.factory
              .create(context)
              .handleUnary(in.name, MessageEnvelope.of(decodedPayload, context.metadata()), context)
            effectToResponse(service, in, effect, service.messageCodec)
          } catch {
            case NonFatal(ex) =>
              // command handler threw an "unexpected" error
              Future.successful(handleUnexpectedException(service, in, ex))
          }
        fut.andThen { case _ =>
          span.foreach(_.end())
        }
      case None =>
        Future.successful(
          ActionResponse(ActionResponse.Response.Failure(Failure(0, "Unknown service: " + in.serviceName))))
    }

  /**
   * Handle a streamed in command. The first message in will contain the request metadata, including the service name
   * and command name. It will not have an associated payload set. This will be followed by zero to many messages in
   * with a payload, but no service name or command name set. The semantics of stream closure in this protocol map 1:1
   * with the semantics of gRPC stream closure, that is, when the client closes the stream, the stream is considered
   * half closed, and the server should eventually, but not necessarily immediately, send a response message with a
   * status code and trailers. If however the server sends a response message before the client closes the stream, the
   * stream is completely closed, and the client should handle this and stop sending more messages. Either the client or
   * the server may cancel the stream at any time, cancellation is indicated through an HTTP2 stream RST message.
   */
  override def handleStreamedIn(in: Source[ActionCommand, NotUsed]): Future[ActionResponse] =
    in.prefixAndTail(1)
      .runWith(Sink.head)
      .flatMap {
        case (Nil, _) =>
          Future.successful(ActionResponse(ActionResponse.Response.Failure(Failure(
            0,
            "Kalix protocol failure: expected command message with service name and command name, but got empty stream"))))
        case (Seq(call), messages) =>
          services.get(call.serviceName) match {
            case Some(service) =>
              try {
                val context = createContext(call, service.messageCodec, None, service.serviceName)
                val effect = service.factory
                  .create(context)
                  .handleStreamedIn(
                    call.name,
                    messages.map { message =>
                      val metadata = MetadataImpl.of(message.metadata.map(_.entries.toVector).getOrElse(Nil))
                      val decodedPayload = service.messageCodec.decodeMessage(
                        message.payload.getOrElse(throw new IllegalArgumentException("No command payload")))
                      MessageEnvelope.of(decodedPayload, metadata)
                    }.asJava,
                    context)
                effectToResponse(service, call, effect, service.messageCodec)
              } catch {
                case NonFatal(ex) =>
                  // command handler threw an "unexpected" error
                  Future.successful(handleUnexpectedException(service, call, ex))
              }
            case None =>
              Future.successful(
                ActionResponse(ActionResponse.Response.Failure(Failure(0, "Unknown service: " + call.serviceName))))
          }
      }

  /**
   * Handle a streamed out command. The input command will contain the service name, command name, request metadata and
   * the command payload. Zero or more replies may be sent, each containing either a direct reply, a forward or a
   * failure, and each may contain many side effects. The stream to the client will be closed when the this stream is
   * closed, with the same status as this stream is closed with. Either the client or the server may cancel the stream
   * at any time, cancellation is indicated through an HTTP2 stream RST message.
   */
  override def handleStreamedOut(in: ActionCommand): Source[ActionResponse, NotUsed] =
    services.get(in.serviceName) match {
      case Some(service) =>
        try {
          val context = createContext(in, service.messageCodec, None, service.serviceName)
          val decodedPayload = service.messageCodec.decodeMessage(
            in.payload.getOrElse(throw new IllegalArgumentException("No command payload")))
          service.factory
            .create(context)
            .handleStreamedOut(in.name, MessageEnvelope.of(decodedPayload, context.metadata()), context)
            .asScala
            .mapAsync(1)(effect => effectToResponse(service, in, effect, service.messageCodec))
            .recover { case NonFatal(ex) =>
              // user stream failed with an "unexpected" error
              handleUnexpectedException(service, in, ex)
            }
            .async
        } catch {
          case NonFatal(ex) =>
            // command handler threw an "unexpected" error
            Source.single(handleUnexpectedException(service, in, ex))
        }
      case None =>
        Source.single(ActionResponse(ActionResponse.Response.Failure(Failure(0, "Unknown service: " + in.serviceName))))
    }

  /**
   * Handle a full duplex streamed command. The first message in will contain the request metadata, including the
   * service name and command name. It will not have an associated payload set. This will be followed by zero to many
   * messages in with a payload, but no service name or command name set. Zero or more replies may be sent, each
   * containing either a direct reply, a forward or a failure, and each may contain many side effects. The semantics of
   * stream closure in this protocol map 1:1 with the semantics of gRPC stream closure, that is, when the client closes
   * the stream, the stream is considered half closed, and the server should eventually, but not necessarily
   * immediately, close the stream with a status code and trailers. If however the server closes the stream with a
   * status code and trailers, the stream is immediately considered completely closed, and no further messages sent by
   * the client will be handled by the server. Either the client or the server may cancel the stream at any time,
   * cancellation is indicated through an HTTP2 stream RST message.
   */
  override def handleStreamed(in: Source[ActionCommand, NotUsed]): Source[ActionResponse, NotUsed] =
    in.prefixAndTail(1)
      .flatMapConcat {
        case (Nil, _) =>
          Source.single(ActionResponse(ActionResponse.Response.Failure(Failure(
            0,
            "Kalix protocol failure: expected command message with service name and command name, but got empty stream"))))
        case (Seq(call), messages) =>
          services.get(call.serviceName) match {
            case Some(service) =>
              try {
                val context = createContext(call, service.messageCodec, None, service.serviceName)
                service.factory
                  .create(context)
                  .handleStreamed(
                    call.name,
                    messages.map { message =>
                      val metadata = MetadataImpl.of(message.metadata.map(_.entries.toVector).getOrElse(Nil))
                      val decodedPayload = service.messageCodec.decodeMessage(
                        message.payload.getOrElse(throw new IllegalArgumentException("No command payload")))
                      MessageEnvelope.of(decodedPayload, metadata)
                    }.asJava,
                    context)
                  .asScala
                  .mapAsync(1)(effect => effectToResponse(service, call, effect, service.messageCodec))
                  .recover { case NonFatal(ex) =>
                    // user stream failed with an "unexpected" error
                    handleUnexpectedException(service, call, ex)
                  }
              } catch {
                case NonFatal(ex) =>
                  // command handler threw an "unexpected" error
                  ErrorHandling.withCorrelationId { correlationId =>
                    service.log.error(s"Failure during handling of command ${call.serviceName}.${call.name}", ex)
                    Source.single(protocolFailure(correlationId))
                  }
              }
            case None =>
              Source.single(
                ActionResponse(ActionResponse.Response.Failure(Failure(0, "Unknown service: " + call.serviceName))))
          }
      }

  private def createContext(
      in: ActionCommand,
      messageCodec: MessageCodec,
      spanContext: Option[SpanContext],
      serviceName: String): ActionContext = {
    val metadata = MetadataImpl.of(in.metadata.map(_.entries.toVector).getOrElse(Nil))
    val updatedMetadata = spanContext.map(metadataWithTracing(metadata, _)).getOrElse(metadata)
    new ActionContextImpl(updatedMetadata, messageCodec, system, serviceName, telemetries(serviceName))
  }

  private def metadataWithTracing(metadata: MetadataImpl, spanContext: SpanContext): Metadata = {
    // remove parent traceparent and tracestate from the metadata so they can be reinjected with current span context
    val l = metadata.entries.filter(m => m.key != TRACE_PARENT_KEY && m.key != TRACE_STATE_KEY).toBuffer

    W3CTraceContextPropagator
      .getInstance()
      .inject(io.opentelemetry.context.Context.current().`with`(Span.wrap(spanContext)), l, TraceInstrumentation.setter)

    if (log.isTraceEnabled)
      log.trace(
        "Updated metadata with trace context: [{}]",
        l.toList.filter(m => m.key == TRACE_PARENT_KEY || m.key == TRACE_STATE_KEY))
    MetadataImpl.of(l.toSeq)
  }

}

case class MessageEnvelopeImpl[T](payload: T, metadata: Metadata) extends MessageEnvelope[T]

/**
 * INTERNAL API
 */
class ActionContextImpl(
    override val metadata: Metadata,
    val messageCodec: MessageCodec,
    val system: ActorSystem,
    serviceName: String,
    instrumentation: Instrumentation)
    extends AbstractContext(system)
    with ActionContext {

  override def eventSubject(): Optional[String] =
    if (metadata.isCloudEvent)
      metadata.asCloudEvent().subject()
    else
      Optional.empty()

  override def getGrpcClient[T](clientClass: Class[T], service: String): T =
    GrpcClients(system).getGrpcClient(clientClass, service)

  override def componentCallMetadata: MetadataImpl = {
    if (metadata.has(Telemetry.TRACE_PARENT_KEY)) {
      MetadataImpl.of(
        List(
          MetadataEntry(
            Telemetry.TRACE_PARENT_KEY,
            MetadataEntry.Value.StringValue(metadata.get(Telemetry.TRACE_PARENT_KEY).get()))))
    } else {
      MetadataImpl.Empty
    }
  }

  override def getOpenTelemetryTracer: Optional[Tracer] =
    Option(instrumentation.getTracer).asJava

  override def getTracer: Tracer =
    instrumentation.getTracer

}
