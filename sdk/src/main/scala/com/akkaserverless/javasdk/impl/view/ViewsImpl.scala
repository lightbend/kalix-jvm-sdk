/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl.view

import java.util.Optional

import scala.compat.java8.OptionConverters._
import scala.util.control.NonFatal

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl.Source
import com.akkaserverless.javasdk.AkkaServerlessRunner.Configuration
import com.akkaserverless.javasdk.Context
import com.akkaserverless.javasdk.Metadata
import com.akkaserverless.javasdk.Service
import com.akkaserverless.javasdk.ServiceCallFactory
import com.akkaserverless.javasdk.impl._
import com.akkaserverless.javasdk.view.HandlerContext
import com.akkaserverless.javasdk.view.ViewContext
import com.akkaserverless.javasdk.view.ViewFactory
import com.akkaserverless.protocol.{view => pv}
import com.google.protobuf.Descriptors
import com.google.protobuf.any.{Any => ScalaPbAny}
import com.google.protobuf.{Any => JavaPbAny}

/** INTERNAL API */
final class ViewService(val factory: Optional[ViewFactory],
                        override val descriptor: Descriptors.ServiceDescriptor,
                        val anySupport: AnySupport,
                        val viewId: String)
    extends Service {

  override def resolvedMethods: Option[Map[String, ResolvedServiceMethod[_, _]]] =
    factory.asScala.collect {
      case resolved: ResolvedEntityFactory => resolved.resolvedMethods
    }

  override final val componentType = pv.Views.name

  override def entityType: String = viewId
}

/** INTERNAL API */
final class ViewsImpl(system: ActorSystem, _services: Map[String, ViewService], rootContext: Context) extends pv.Views {

  private final val services = _services.iterator.toMap
  private final val log = Logging(system.eventStream, this.getClass)

  /**
   * Handle a full duplex streamed session. One stream will be established per incoming message to the view service.
   *
   *
   * The first message is ReceiveEvent and contain the request metadata, including the service name and command
   * name.
   */
  override def handle(
      in: akka.stream.scaladsl.Source[pv.ViewStreamIn, akka.NotUsed]
  ): akka.stream.scaladsl.Source[pv.ViewStreamOut, akka.NotUsed] =
    // FIXME: see akkaserverless-framework/issues/186 and akkaserverless-framework/issues/188
    // It is currently only implemented to support one request (ReceiveEvent) with one response (Upsert).
    // The intention, and reason for full-duplex streaming, is that there should be able to have an interaction
    // with two main types of operations, loads, and updates, and with
    // each load there is an associated continuation, which in turn may return more operations, including more loads,
    // and so on recursively.
    in.prefixAndTail(1)
      .flatMapConcat {
        case (Seq(pv.ViewStreamIn(pv.ViewStreamIn.Message.Receive(receiveEvent), _)), tail) =>
          services.get(receiveEvent.serviceName) match {
            case Some(service: ViewService) =>
              if (!service.factory.isPresent)
                throw new IllegalArgumentException(
                  s"Unexpected call to service [${receiveEvent.serviceName}] with viewId [${service.viewId}]."
                )

              val handler = service.factory.get.create(new ViewContextImpl(service.viewId))

              val state: Optional[JavaPbAny] =
                receiveEvent.bySubjectLookupResult match {
                  case Some(row) => row.value.map(ScalaPbAny.toJavaProto).asJava
                  case None => Optional.empty
                }

              val commandName = receiveEvent.commandName
              val msg = ScalaPbAny.toJavaProto(receiveEvent.payload.get)
              val metadata = new MetadataImpl(receiveEvent.metadata.map(_.entries.toVector).getOrElse(Nil))
              val sourceEntityId = metadata.subject()
              val context = new HandlerContextImpl(service.viewId, sourceEntityId, commandName, metadata, state)

              val reply = try {
                handler.handle(msg, context)
              } catch {
                case e: ViewException => throw e
                case NonFatal(error) =>
                  throw ViewException(context, s"View unexpected failure: ${error.getMessage}")
              }

              Source.single(replyToOut(reply, receiveEvent))

            case None =>
              val errMsg = s"Unknown service: ${receiveEvent.serviceName}"
              log.error(errMsg)
              Source.failed(new RuntimeException(errMsg))
          }

        case (Nil, _) =>
          val errMsg =
            "Akka Serverless protocol failure: expected ReceiveEvent message with service name and command name, but got empty stream"
          log.error(errMsg)
          Source.failed(
            throw new RuntimeException(errMsg)
          )

        case (Seq(pv.ViewStreamIn(other, _)), _) =>
          val errMsg =
            s"Akka Serverless protocol failure: expected ReceiveEvent message, but got ${other.getClass.getName}"
          Source.failed(
            throw new RuntimeException(errMsg)
          )
      }

  private def replyToOut(reply: Optional[JavaPbAny], receiveEvent: pv.ReceiveEvent): pv.ViewStreamOut = {
    val table = receiveEvent.initialTable
    val key = receiveEvent.key
    if (reply.isPresent) {
      val outMessage = pv.ViewStreamOut.Message.Upsert(
        pv.Upsert(Some(pv.Row(table, key, Some(ScalaPbAny.fromJavaProto(reply.get)))))
      )
      pv.ViewStreamOut(outMessage)
    } else {
      // FIXME it should be possible to ignore the event without new Upsert
      pv.Upsert(Some(pv.Row(table, key, receiveEvent.payload)))
      throw new IllegalArgumentException("Empty reply not supported yet.")
    }
  }

  trait AbstractContext extends ViewContext {
    override def serviceCallFactory(): ServiceCallFactory = rootContext.serviceCallFactory()
  }

  private final class HandlerContextImpl(override val viewId: String,
                                         override val sourceEntityId: Optional[String],
                                         override val commandName: String,
                                         override val metadata: Metadata,
                                         override val state: Optional[JavaPbAny])
      extends HandlerContext
      with AbstractContext
      with StateContext

  private final class ViewContextImpl(override val viewId: String) extends ViewContext with AbstractContext

}

/** INTERNAL API */
trait StateContext {
  def state: Optional[JavaPbAny]
}
