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

package com.akkaserverless.javasdk.impl.action

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import com.akkaserverless.javasdk._
import com.akkaserverless.javasdk.action._
import com.akkaserverless.javasdk.impl.AnySupport
import com.akkaserverless.javasdk.impl._
import com.akkaserverless.protocol.action.ActionCommand
import com.akkaserverless.protocol.action.ActionResponse
import com.akkaserverless.protocol.action.Actions
import com.akkaserverless.protocol.component
import com.akkaserverless.protocol.component.Failure
import com.google.protobuf.Descriptors
import com.google.protobuf.any.{ Any => ScalaPbAny }
import com.google.protobuf.{ Any => JavaPbAny }
import java.util.Optional

import scala.collection.immutable
import scala.concurrent.Future
import scala.jdk.CollectionConverters.SeqHasAsJava

import com.akkaserverless.javasdk.impl.ActionFactory

final class ActionService(
    val factory: ActionFactory,
    override val descriptor: Descriptors.ServiceDescriptor,
    val anySupport: AnySupport)
    extends Service {

  override def resolvedMethods: Option[Map[String, ResolvedServiceMethod[_, _]]] =
    factory match {
      case resolved: ResolvedEntityFactory => Some(resolved.resolvedMethods)
      case _                               => None
    }

  override final val componentType = Actions.name
}

final class ActionsImpl(_system: ActorSystem, services: Map[String, ActionService], rootContext: Context)
    extends Actions {

  import _system.dispatcher
  implicit val system: ActorSystem = _system

  private object creationContext
      extends AbstractContext(rootContext.serviceCallFactory(), system)
      with ActionCreationContext

  private def toJavaPbAny(any: Option[ScalaPbAny]) =
    any.fold(JavaPbAny.getDefaultInstance)(ScalaPbAny.toJavaProto)

  private def effectToResponse(effect: Action.Effect[_], anySupport: AnySupport): Future[ActionResponse] = {
    import ActionEffectImpl._
    effect match {
      case ReplyEffect(message, metadata, sideEffects) =>
        val response =
          component.Reply(Some(ScalaPbAny.fromJavaProto(anySupport.encodeJava(message))), metadata.flatMap(toProtocol))
        Future.successful(ActionResponse(ActionResponse.Response.Reply(response), toProtocol(sideEffects)))
      case ForwardEffect(forward, sideEffects) =>
        val response = component.Forward(
          forward.ref().method().getService.getFullName,
          forward.ref().method().getName,
          Some(ScalaPbAny.fromJavaProto(forward.message())),
          toProtocol(forward.metadata()))
        Future.successful(ActionResponse(ActionResponse.Response.Forward(response), toProtocol(sideEffects)))
      case AsyncEffect(futureEffect, sideEffects) =>
        futureEffect.flatMap { effect =>
          val withSurroundingSideEffects = effect.addSideEffects(sideEffects.asJava)
          effectToResponse(withSurroundingSideEffects, anySupport)
        }
      case ErrorEffect(description, sideEffects) =>
        Future.successful(
          ActionResponse(ActionResponse.Response.Failure(Failure(description = description)), toProtocol(sideEffects)))
      case NoReply(sideEffects) =>
        Future.successful(ActionResponse(ActionResponse.Response.Empty, toProtocol(sideEffects)))
      case unknown =>
        throw new IllegalArgumentException(s"Unknown Action.Effect type ${unknown.getClass}")
    }
  }

  private def toProtocol(sideEffects: immutable.Seq[SideEffect]): Seq[component.SideEffect] =
    sideEffects.map { sideEffect =>
      component.SideEffect(
        sideEffect.serviceCall().ref().method().getService.getFullName,
        sideEffect.serviceCall().ref().method().getName,
        Some(ScalaPbAny.fromJavaProto(sideEffect.serviceCall().message())),
        sideEffect.synchronous(),
        toProtocol(sideEffect.serviceCall().metadata()))
    }

  private def toProtocol(metadata: com.akkaserverless.javasdk.Metadata): Option[component.Metadata] =
    metadata match {
      case impl: MetadataImpl if impl.entries.nonEmpty =>
        Some(component.Metadata(impl.entries))
      case _: MetadataImpl => None
      case other =>
        throw new RuntimeException(s"Unknown metadata implementation: ${other.getClass}, cannot send")
    }

  /**
   * Handle a unary command. The input command will contain the service name, command name, request metadata and the
   * command payload. The reply may contain a direct reply, a forward or a failure, and it may contain many side
   * effects.
   */
  override def handleUnary(in: ActionCommand): Future[ActionResponse] =
    services.get(in.serviceName) match {
      case Some(service) =>
        val context = createContext(in)
        val decodedPayload = service.anySupport.decode(toJavaPbAny(in.payload))
        val effect = service.factory
          .create(creationContext)
          .handleUnary(in.name, MessageEnvelope.of(decodedPayload, context.metadata()), context)
        effectToResponse(effect, service.anySupport)
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
            "Akka Serverless protocol failure: expected command message with service name and command name, but got empty stream"))))
        case (Seq(call), messages) =>
          services.get(call.serviceName) match {
            case Some(service) =>
              val effect = service.factory
                .create(creationContext)
                .handleStreamedIn(
                  call.name,
                  messages.map { message =>
                    val metadata = new MetadataImpl(message.metadata.map(_.entries.toVector).getOrElse(Nil))
                    val decodedPayload = service.anySupport.decode(toJavaPbAny(message.payload))
                    MessageEnvelope.of(decodedPayload, metadata)
                  }.asJava,
                  createContext(call))
              effectToResponse(effect, service.anySupport)
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
        val context = createContext(in)
        val decodedPayload = service.anySupport.decode(toJavaPbAny(in.payload))
        service.factory
          .create(creationContext)
          .handleStreamedOut(in.name, MessageEnvelope.of(decodedPayload, context.metadata()), context)
          .asScala
          .mapAsync(1)(effect => effectToResponse(effect, service.anySupport))
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
   * immediately, close the streamage with a status code and trailers. If however the server closes the stream with a
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
            "Akka Serverless protocol failure: expected command message with service name and command name, but got empty stream"))))
        case (Seq(call), messages) =>
          services.get(call.serviceName) match {
            case Some(service) =>
              service.factory
                .create(creationContext)
                .handleStreamed(
                  call.name,
                  messages.map { message =>
                    val metadata = new MetadataImpl(message.metadata.map(_.entries.toVector).getOrElse(Nil))
                    val decodedPayload = service.anySupport.decode(toJavaPbAny(message.payload))
                    MessageEnvelope.of(decodedPayload, metadata)
                  }.asJava,
                  createContext(call))
                .asScala
                .mapAsync(1)(effect => effectToResponse(effect, service.anySupport))
            case None =>
              Source.single(
                ActionResponse(ActionResponse.Response.Failure(Failure(0, "Unknown service: " + call.serviceName))))
          }
      }

  private def createContext(in: ActionCommand): ActionContext = {
    val metadata = new MetadataImpl(in.metadata.map(_.entries.toVector).getOrElse(Nil))
    new ActionContextImpl(metadata)
  }

  class ActionContextImpl(override val metadata: Metadata)
      extends AbstractContext(rootContext.serviceCallFactory(), system)
      with ActionContext {

    override def eventSubject(): Optional[String] =
      if (metadata.isCloudEvent)
        metadata.asCloudEvent().subject()
      else
        Optional.empty()
  }
}

case class MessageEnvelopeImpl[T](payload: T, metadata: Metadata) extends MessageEnvelope[T]
