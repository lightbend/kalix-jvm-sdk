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

package com.akkaserverless.scalasdk.testkit.impl

import com.akkaserverless.javasdk.impl.DeferredCallImpl
import com.akkaserverless.javasdk.impl.effect.SideEffectImpl
import com.akkaserverless.javasdk.impl.MetadataImpl
import com.akkaserverless.scalasdk.impl.ScalaDeferredCallAdapter
import com.akkaserverless.scalasdk.impl.ScalaSideEffectAdapter
import com.akkaserverless.scalasdk.impl.valueentity.ValueEntityEffectImpl
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ValueEntityResultSpec extends AnyWordSpec with Matchers {

  "Value Entity Results" must {
    "extract side effects" in {
      val replyWithSideEffectResult = new ValueEntityResultImpl[String](
        ValueEntityEffectImpl()
          .reply("reply")
          .addSideEffects(ScalaSideEffectAdapter(SideEffectImpl(
            DeferredCallImpl[String, Any]("request", MetadataImpl.Empty, "full.service.Name", "MethodName", () => ???),
            synchronous = false)) :: Nil))

      replyWithSideEffectResult.isReply should ===(true)
      replyWithSideEffectResult.sideEffects should have size 1
    }

    "extract forward details" in {
      val forwardResult = new ValueEntityResultImpl[String](ValueEntityEffectImpl().forward(ScalaDeferredCallAdapter(
        DeferredCallImpl[String, String]("request", MetadataImpl.Empty, "full.service.Name", "MethodName", () => ???))))

      forwardResult.isForward should ===(true)
      forwardResult.forwardedTo.message should ===("request")
    }

  }

}
