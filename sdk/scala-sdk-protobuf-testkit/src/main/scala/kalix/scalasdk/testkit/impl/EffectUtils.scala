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

package kalix.scalasdk.testkit.impl

import kalix.javasdk.{ SideEffect => JavaSideEffect }
import kalix.javasdk.impl.GrpcDeferredCall
import kalix.javasdk.impl.effect.ErrorReplyImpl
import kalix.javasdk.impl.effect.ForwardReplyImpl
import kalix.javasdk.impl.effect.MessageReplyImpl
import kalix.javasdk.impl.effect.NoSecondaryEffectImpl
import kalix.javasdk.impl.effect.{ SecondaryEffectImpl => JavaSecondaryEffectImpl }
import kalix.scalasdk.testkit.DeferredCallDetails

import scala.collection.immutable.Seq

private[kalix] object EffectUtils {
  def toDeferredCallDetails(sideEffects: Seq[JavaSideEffect]): Seq[DeferredCallDetails[_, _]] = {
    sideEffects.map { sideEffect =>
      TestKitDeferredCall(sideEffect.call.asInstanceOf[GrpcDeferredCall[_, _]])
    }
  }

  def forwardDetailsFor[R](secondaryEffect: JavaSecondaryEffectImpl): DeferredCallDetails[_, R] =
    secondaryEffect match {
      case reply: ForwardReplyImpl[R @unchecked] =>
        TestKitDeferredCall(reply.deferredCall.asInstanceOf[GrpcDeferredCall[_, R]])
      case _ => throw new IllegalArgumentException(s"Expected a forward effect but was [${nameFor(secondaryEffect)}]")
    }

  def nameFor(secondaryEffect: JavaSecondaryEffectImpl): String =
    secondaryEffect match {
      case _: MessageReplyImpl[_]   => "reply"
      case _: ForwardReplyImpl[_]   => "forward"
      case _: ErrorReplyImpl[_]     => "error"
      case _: NoSecondaryEffectImpl => "no effect"
    }
}
