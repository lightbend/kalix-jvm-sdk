/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.view

import java.util.Optional
import scala.compat.java8.OptionConverters._
import scala.util.control.NonFatal
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import kalix.javasdk.impl.{ Service, ViewFactory }
import kalix.javasdk.Metadata
import kalix.javasdk.impl._
import kalix.javasdk.view.{ UpdateContext, ViewContext, ViewCreationContext, ViewOptions }
import kalix.protocol.{ view => pv }
import com.google.protobuf.Descriptors
import com.google.protobuf.any.{ Any => ScalaPbAny }
import org.slf4j.LoggerFactory

/** INTERNAL API */
final class ViewService(
    val factory: Optional[ViewFactory],
    override val descriptor: Descriptors.ServiceDescriptor,
    override val additionalDescriptors: Array[Descriptors.FileDescriptor],
    val messageCodec: MessageCodec,
    val viewId: String,
    val viewOptions: Option[ViewOptions])
    extends Service {

  def this(
      factory: Optional[ViewFactory],
      descriptor: Descriptors.ServiceDescriptor,
      additionalDescriptors: Array[Descriptors.FileDescriptor],
      messageCodec: MessageCodec,
      viewId: String,
      viewOptions: ViewOptions) =
    this(factory, descriptor, additionalDescriptors, messageCodec, viewId, Some(viewOptions))

  override def resolvedMethods: Option[Map[String, ResolvedServiceMethod[_, _]]] =
    factory.asScala.collect { case resolved: ResolvedEntityFactory =>
      resolved.resolvedMethods
    }

  override final val componentType = pv.Views.name

  override def serviceName: String = viewId

  override def componentOptions: Option[ComponentOptions] = viewOptions
}

object ViewsImpl {
  private val log = LoggerFactory.getLogger(classOf[ViewsImpl])
}

/** INTERNAL API */
final class ViewsImpl(system: ActorSystem, _services: Map[String, ViewService]) extends pv.Views {
  import ViewsImpl.log

  private final val services = _services.iterator.toMap

  /**
   * Handle a full duplex streamed session. One stream will be established per incoming message to the view service.
   *
   * The first message is ReceiveEvent and contain the request metadata, including the service name and command name.
   */
  override def handle(in: akka.stream.scaladsl.Source[pv.ViewStreamIn, akka.NotUsed])
      : akka.stream.scaladsl.Source[pv.ViewStreamOut, akka.NotUsed] =
    // FIXME: see kalix-proxy/issues/209 and kalix-proxy/issues/207
    // It is currently only implemented to support one request (ReceiveEvent) with one response (Upsert).
    // The intention, and reason for full-duplex streaming, is that there should be able to have an interaction
    // with two main types of operations, loads, and updates, and with
    // each load there is an associated continuation, which in turn may return more operations, including more loads,
    // and so on recursively.
    in.prefixAndTail(1)
      .flatMapConcat {
        case (Seq(pv.ViewStreamIn(pv.ViewStreamIn.Message.Receive(receiveEvent), _)), _) =>
          services.get(receiveEvent.serviceName) match {
            case Some(service: ViewService) =>
              if (!service.factory.isPresent)
                throw new IllegalArgumentException(
                  s"Unexpected call to service [${receiveEvent.serviceName}] with viewId [${service.viewId}]: " +
                  "this view has `transform_updates=false` set, so updates should be handled entirely by the proxy " +
                  "and not reach the user function")

              // FIXME should we really create a new handler instance per incoming command ???
              val handler = service.factory.get.create(new ViewContextImpl(service.viewId))

              val state: Option[Any] =
                receiveEvent.bySubjectLookupResult.flatMap(row =>
                  row.value.map(scalaPb => service.messageCodec.decodeMessage(scalaPb)))

              val commandName = receiveEvent.commandName
              val msg = service.messageCodec.decodeMessage(receiveEvent.payload.get)
              val metadata = MetadataImpl.of(receiveEvent.metadata.map(_.entries.toVector).getOrElse(Nil))
              val context = new UpdateContextImpl(service.viewId, commandName, metadata)

              val effect =
                try {
                  handler._internalHandleUpdate(state, msg, context)
                } catch {
                  case e: ViewException => throw e
                  case NonFatal(error) =>
                    throw ViewException(context, s"View unexpected failure: ${error.getMessage}", Some(error))
                }

              effect match {
                case ViewUpdateEffectImpl.Update(newState) =>
                  if (newState == null)
                    throw ViewException(context, "updateState with null state is not allowed.", None)
                  val serializedState = ScalaPbAny.fromJavaProto(service.messageCodec.encodeJava(newState))
                  val upsert = pv.Upsert(Some(pv.Row(value = Some(serializedState))))
                  val out = pv.ViewStreamOut(pv.ViewStreamOut.Message.Upsert(upsert))
                  Source.single(out)
                case ViewUpdateEffectImpl.Delete =>
                  val delete = pv.Delete()
                  val out = pv.ViewStreamOut(pv.ViewStreamOut.Message.Delete(delete))
                  Source.single(out)
                case ViewUpdateEffectImpl.Ignore =>
                  // ignore incoming event
                  val upsert = pv.Upsert(None)
                  val out = pv.ViewStreamOut(pv.ViewStreamOut.Message.Upsert(upsert))
                  Source.single(out)
                case ViewUpdateEffectImpl.Error(e) =>
                  Source.failed(new RuntimeException(e))
              }

            case None =>
              val errMsg = s"Unknown service: ${receiveEvent.serviceName}"
              log.error(errMsg)
              Source.failed(new RuntimeException(errMsg))
          }

        case (Seq(), _) =>
          log.warn("View stream closed before init.")
          Source.empty[pv.ViewStreamOut]

        case (Seq(pv.ViewStreamIn(other, _)), _) =>
          val errMsg =
            s"Kalix protocol failure: expected ReceiveEvent message, but got ${other.getClass.getName}"
          Source.failed(new RuntimeException(errMsg))
      }
      .async

  private final class UpdateContextImpl(
      override val viewId: String,
      override val eventName: String,
      override val metadata: Metadata)
      extends AbstractContext(system)
      with UpdateContext {

    override def eventSubject(): Optional[String] =
      if (metadata.isCloudEvent)
        metadata.asCloudEvent().subject()
      else
        Optional.empty()
  }

  private final class ViewContextImpl(override val viewId: String)
      extends AbstractContext(system)
      with ViewContext
      with ViewCreationContext

}
