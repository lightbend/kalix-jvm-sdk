/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.testkit.impl

import kalix.javasdk.impl.GrpcDeferredCall
import kalix.javasdk.impl.MetadataImpl
import kalix.javasdk.impl.effect.ForwardReplyImpl
import kalix.javasdk.impl.effect.MessageReplyImpl
import kalix.javasdk.impl.effect.SideEffectImpl
import kalix.scalasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class EventSourcedResultSpec extends AnyWordSpec with Matchers {

  "Event Sourced Entity Results" must {

    "extract side effects" in {
      val replyWithSideEffectResult = new EventSourcedResultImpl[String, String](
        EventSourcedEntityEffectImpl().reply("not actually used here"),
        "state",
        MessageReplyImpl(
          "reply", // pretend it was evaluated, in practice done by the generated testkit
          MetadataImpl.Empty,
          Vector(
            SideEffectImpl(
              GrpcDeferredCall[String, Any]("request", MetadataImpl.Empty, "full.service.Name", "MethodName", _ => ???),
              synchronous = false))))

      replyWithSideEffectResult.isReply should ===(true)
      replyWithSideEffectResult.sideEffects should have size 1
    }

    "extract forward details" in {
      val forwardResult = new EventSourcedResultImpl[String, String](
        EventSourcedEntityEffectImpl().reply("not actually used here"),
        "state",
        ForwardReplyImpl(
          deferredCall =
            GrpcDeferredCall[String, Any]("request", MetadataImpl.Empty, "full.service.Name", "MethodName", _ => ???),
          sideEffects = Vector.empty))

      forwardResult.isForward should ===(true)
      forwardResult.forwardedTo.message should ===("request")
    }

  }

}
