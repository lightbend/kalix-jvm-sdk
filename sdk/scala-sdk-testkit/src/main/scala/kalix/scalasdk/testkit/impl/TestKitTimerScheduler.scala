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

package kalix.scalasdk.testkit.impl

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

import akka.Done
import kalix.javasdk.impl.DeferredCallImpl
import kalix.scalasdk.DeferredCall
import kalix.scalasdk.impl.ScalaDeferredCallAdapter
import kalix.scalasdk.testkit.SingleTimerDetails
import kalix.scalasdk.timer.TimerScheduler

class TestKitTimerScheduler extends TimerScheduler {

  private var _singleTimers: Seq[SingleTimerDetails] = Seq.empty
  private var _timerCancellations: Seq[String] = Seq.empty

  def singleTimers: Seq[SingleTimerDetails] = _singleTimers
  def timerCancellations: Seq[String] = _timerCancellations

  override def startSingleTimer[I, O](
      name: String,
      delay: FiniteDuration,
      deferredCall: DeferredCall[I, O]): Future[Done] = {

    val deferredCallImpl =
      deferredCall match {
        case ScalaDeferredCallAdapter(javaSdkDeferredCall: DeferredCallImpl[_, _]) => javaSdkDeferredCall
        case other =>
          throw new RuntimeException(
            s"Incompatible DeferredCall instance. Found ${other.getClass}, expecting ${classOf[DeferredCallImpl[_, _]].getName}")

      }
    _singleTimers = _singleTimers :+ TestKitSingleTimerDetails(name, delay, TestKitDeferredCall(deferredCallImpl))

    Future.successful(Done)
  }

  override def cancel(name: String): Future[Done] = {
    _timerCancellations = _timerCancellations :+ name
    Future.successful(Done)
  }
}
