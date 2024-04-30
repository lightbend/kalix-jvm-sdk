/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.timer

import java.time.Duration
import java.util.concurrent.CompletionStage

import scala.jdk.CollectionConverters._
import scala.jdk.FutureConverters.FutureOps

import akka.Done
import akka.actor.ActorSystem
import akka.grpc.scaladsl.SingleResponseRequestBuilder
import com.google.protobuf.any.{ Any => ScalaPbAny }
import com.google.protobuf.duration.{ Duration => ProtoDuration }
import com.google.protobuf.wrappers.StringValue
import kalix.javasdk.DeferredCall
import kalix.javasdk.Metadata
import kalix.javasdk.impl.GrpcClients
import kalix.javasdk.impl.GrpcDeferredCall
import kalix.javasdk.impl.MessageCodec
import kalix.javasdk.impl.RestDeferredCall
import kalix.javasdk.timer.TimerScheduler
import kalix.timers.timers.Call
import kalix.timers.timers.SingleTimer
import kalix.timers.timers.TimerService
import kalix.timers.timers.TimerServiceClient

/** INTERNAL API */
private[kalix] final class TimerSchedulerImpl(
    val messageCodec: MessageCodec,
    val system: ActorSystem,
    val metadata: Metadata)
    extends TimerScheduler {

  override def startSingleTimer[I, O](
      name: String,
      delay: Duration,
      deferredCall: DeferredCall[I, O]): CompletionStage[Done] =
    startSingleTimer(name, delay, 0, deferredCall)

  override def startSingleTimer[I, O](
      name: String,
      delay: Duration,
      maxRetries: Int,
      deferredCall: DeferredCall[I, O]): CompletionStage[Done] = {
    val timerServiceClient =
      GrpcClients(system).getProxyGrpcClient(classOf[TimerService]).asInstanceOf[TimerServiceClient]

    val call = deferredCall match {
      case grpcDeferredCall: GrpcDeferredCall[I, O] =>
        Call(
          grpcDeferredCall.fullServiceName,
          grpcDeferredCall.methodName,
          Some(messageCodec.encodeScala(grpcDeferredCall.message)))
      case restDeferredCall: RestDeferredCall[I, O] =>
        Call(
          restDeferredCall.fullServiceName,
          restDeferredCall.methodName,
          Some(restDeferredCall.message.asInstanceOf[ScalaPbAny]))
      case _ =>
        // should never happen, but needs to make compiler happy
        throw new IllegalStateException("Unknown DeferredCall implementation")
    }

    val singleTimer = SingleTimer(name, Some(call), Some(ProtoDuration(delay)), maxRetries)
    addHeaders(timerServiceClient.addSingle(), metadata).invoke(singleTimer).asJava.thenApply(_ => Done)
  }

  def cancel(name: String): CompletionStage[Done] = {
    val timerServiceClient =
      GrpcClients(system).getProxyGrpcClient(classOf[TimerService]).asInstanceOf[TimerServiceClient]
    addHeaders(timerServiceClient.remove(), metadata).invoke(StringValue(name)).asJava.thenApply(_ => Done)
  }

  private def addHeaders[I, O](
      callBuilder: SingleResponseRequestBuilder[I, O],
      metadata: Metadata): SingleResponseRequestBuilder[I, O] = {
    metadata.asScala.foldLeft(callBuilder) { case (builder, entry) =>
      if (entry.isText) builder.addHeader(entry.getKey, entry.getValue)
      else builder
    }
  }

}
