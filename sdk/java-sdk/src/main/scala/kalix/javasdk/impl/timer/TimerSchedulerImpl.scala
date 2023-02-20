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

package kalix.javasdk.impl.timer

import java.time.Duration
import java.util.concurrent.CompletionStage

import scala.jdk.FutureConverters.FutureOps

import akka.Done
import akka.actor.ActorSystem
import com.google.protobuf.duration.{ Duration => ProtoDuration }
import com.google.protobuf.wrappers.StringValue
import com.google.protobuf.any.{ Any => ScalaPbAny }
import kalix.javasdk.DeferredCall
import kalix.javasdk.impl.{ GrpcClients, GrpcDeferredCall, MessageCodec, RestDeferredCall }
import kalix.javasdk.timer.TimerScheduler
import kalix.timers.timers.Call
import kalix.timers.timers.SingleTimer
import kalix.timers.timers.TimerService

/** INTERNAL API */
private[kalix] final class TimerSchedulerImpl(messageCodec: MessageCodec, system: ActorSystem) extends TimerScheduler {

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
    val timerServiceClient = GrpcClients(system).getProxyGrpcClient(classOf[TimerService])

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
    }

    val singleTimer = SingleTimer(name, Some(call), Some(ProtoDuration(delay)), maxRetries)

    timerServiceClient.addSingle(singleTimer).asJava.thenApply(_ => Done)
  }

  def cancel(name: String): CompletionStage[Done] = {
    val timerServiceClient = GrpcClients(system).getProxyGrpcClient(classOf[TimerService])
    timerServiceClient.remove(StringValue(name)).asJava.thenApply(_ => Done)
  }

}
