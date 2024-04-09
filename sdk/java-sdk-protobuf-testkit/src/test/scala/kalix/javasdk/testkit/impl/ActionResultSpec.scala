/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.testkit.impl

import kalix.javasdk.impl.GrpcDeferredCall
import kalix.javasdk.impl.MetadataImpl
import kalix.javasdk.impl.action.ActionEffectImpl
import kalix.javasdk.impl.effect.SideEffectImpl
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ActionResultSpec extends AnyWordSpec with Matchers {

  "Action Results" must {
    "extract side effects" in {
      val replyWithSideEffectResult = new ActionResultImpl[String](
        ActionEffectImpl.Builder
          .reply("reply")
          .addSideEffect(SideEffectImpl(
            GrpcDeferredCall[String, Any]("request", MetadataImpl.Empty, "full.service.Name", "MethodName", _ => ???),
            synchronous = false)))

      replyWithSideEffectResult.isReply() should ===(true)
      replyWithSideEffectResult.getSideEffects().size() should ===(1)
    }

    "extract forward details" in {
      val forwardResult = new ActionResultImpl[String](
        ActionEffectImpl.Builder.forward(
          GrpcDeferredCall[String, String]("request", MetadataImpl.Empty, "full.service.Name", "MethodName", _ => ???)))

      forwardResult.isForward() should ===(true)
      forwardResult.getForward().getMessage should ===("request")
    }
  }

}
