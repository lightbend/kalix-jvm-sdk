/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.impl.timer

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.jdk.DurationConverters.ScalaDurationOps

import akka.Done
import akka.actor.ActorSystem
import akka.grpc.scaladsl.SingleResponseRequestBuilder
import com.google.protobuf.duration.{ Duration => ProtoDuration }
import com.google.protobuf.wrappers.StringValue
import kalix.javasdk.impl.GrpcDeferredCall
import kalix.javasdk.impl.GrpcClients
import kalix.javasdk.impl.MessageCodec
import kalix.scalasdk.DeferredCall
import kalix.scalasdk.Metadata
import kalix.scalasdk.impl.ScalaDeferredCallAdapter
import kalix.scalasdk.timer.TimerScheduler
import kalix.timers.timers.Call
import kalix.timers.timers.SingleTimer
import kalix.timers.timers.TimerService
import kalix.timers.timers.TimerServiceClient

/** INTERNAL API */
private[kalix] final class TimerSchedulerImpl(messageCodec: MessageCodec, system: ActorSystem, metadata: Metadata)
    extends TimerScheduler {

  override def startSingleTimer[I, O](
      name: String,
      delay: FiniteDuration,
      deferredCall: DeferredCall[I, O]): Future[Done] =
    startSingleTimer(name, delay, 0, deferredCall)

  override def startSingleTimer[I, O](
      name: String,
      delay: FiniteDuration,
      maxRetries: Int,
      deferredCall: DeferredCall[I, O]): Future[Done] = {
    val timerServiceClient =
      GrpcClients(system).getProxyGrpcClient(classOf[TimerService]).asInstanceOf[TimerServiceClient]

    val deferredCallImpl =
      deferredCall match {
        case ScalaDeferredCallAdapter(deferredCallImpl: GrpcDeferredCall[I, O] @unchecked) => deferredCallImpl
        // should not happen as we always need to pass ScalaDeferredCallAdapter(DeferredCallImpl)
        case other =>
          throw new RuntimeException(
            s"Incompatible DeferredCall instance. Found ${other.getClass}, expecting ${classOf[GrpcDeferredCall[_, _]].getName}")
      }

    val call =
      Call(
        deferredCallImpl.fullServiceName,
        deferredCallImpl.methodName,
        Some(messageCodec.encodeScala(deferredCall.message)))

    val singleTimer = SingleTimer(name, Some(call), Some(ProtoDuration(delay.toJava)))
    addHeaders(timerServiceClient.addSingle(), metadata)
      .invoke(singleTimer)
      .map(_ => Done)(ExecutionContext.parasitic)
  }

  override def cancel(name: String): Future[Done] = {
    val timerServiceClient =
      GrpcClients(system).getProxyGrpcClient(classOf[TimerService]).asInstanceOf[TimerServiceClient]
    addHeaders(timerServiceClient.remove(), metadata)
      .invoke(StringValue(name))
      .map(_ => Done)(ExecutionContext.parasitic)
  }

  private def addHeaders[I, O](
      callBuilder: SingleResponseRequestBuilder[I, O],
      metadata: Metadata): SingleResponseRequestBuilder[I, O] = {
    metadata.foldLeft(callBuilder) { case (builder, entry) =>
      if (entry.isText) builder.addHeader(entry.key, entry.value)
      else builder
    }
  }
}
