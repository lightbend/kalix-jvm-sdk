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

package com.akkaserverless.javasdk.testkit.impl

import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity.Effect
import com.akkaserverless.javasdk.impl.effect.ErrorReplyImpl
import com.akkaserverless.javasdk.impl.effect.ForwardReplyImpl
import com.akkaserverless.javasdk.impl.effect.MessageReplyImpl
import com.akkaserverless.javasdk.impl.effect.NoReply
import com.akkaserverless.javasdk.impl.effect.NoSecondaryEffectImpl
import com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl
import com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl.EmitEvents
import com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl.NoPrimaryEffect

/**
 * Internal API, called from generated testkit classes.
 */
class AkkaServerlessTestKitHelper[S] {

  def getEvents(effect: Effect[_]): List[Any] = {
    effect match {
      case ese: EventSourcedEntityEffectImpl[S] =>
        ese.primaryEffect match {
          case ee: EmitEvents => ee.event.toList
          case _: NoPrimaryEffect.type => List()
        }
    }
  }

  def getReply[R](effect: Effect[R], state: S): R = {
    effect match {
      case ese: EventSourcedEntityEffectImpl[S] =>
        val reply = ese.secondaryEffect(state)
        reply match {
          case mri: MessageReplyImpl[R @unchecked] => mri.message
          case fr: ForwardReplyImpl[R @unchecked] => throw new NotImplementedError(fr.toString)
          case er: ErrorReplyImpl[R @unchecked] => throw new NotImplementedError(er.toString)
          case nr: NoReply[R @unchecked] => throw new IllegalStateException("This effect does not include a reply")
          case NoSecondaryEffectImpl => throw new IllegalStateException("This effect does not include a reply")
        }
    }
  }

}
