/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.testkit.impl

import kalix.javasdk.impl.GrpcDeferredCall
import kalix.javasdk.impl.effect.SideEffectImpl
import kalix.javasdk.impl.MetadataImpl
import kalix.scalasdk.impl.ScalaDeferredCallAdapter
import kalix.scalasdk.impl.ScalaSideEffectAdapter
import kalix.scalasdk.impl.valueentity.ValueEntityEffectImpl
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ValueEntityResultSpec extends AnyWordSpec with Matchers {

  "Value Entity Results" must {
    "extract side effects" in {
      val replyWithSideEffectResult = new ValueEntityResultImpl[String](
        ValueEntityEffectImpl()
          .reply("reply")
          .addSideEffects(ScalaSideEffectAdapter(SideEffectImpl(
            GrpcDeferredCall[String, Any]("request", MetadataImpl.Empty, "full.service.Name", "MethodName", _ => ???),
            synchronous = false)) :: Nil))

      replyWithSideEffectResult.isReply should ===(true)
      replyWithSideEffectResult.sideEffects should have size 1
    }

    "extract forward details" in {
      val forwardResult = new ValueEntityResultImpl[String](ValueEntityEffectImpl().forward(ScalaDeferredCallAdapter(
        GrpcDeferredCall[String, String]("request", MetadataImpl.Empty, "full.service.Name", "MethodName", _ => ???))))

      forwardResult.isForward should ===(true)
      forwardResult.forwardedTo.message should ===("request")
    }

  }

}
