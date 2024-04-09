/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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
