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

import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity
import com.akkaserverless.javasdk.impl.effect.ErrorReplyImpl
import com.akkaserverless.javasdk.impl.effect.ForwardReplyImpl
import com.akkaserverless.javasdk.impl.effect.MessageReplyImpl
import com.akkaserverless.javasdk.impl.effect.NoReply
import com.akkaserverless.javasdk.impl.effect.NoSecondaryEffectImpl
import com.akkaserverless.javasdk.impl.effect.SecondaryEffectImpl
import com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl
import com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl.EmitEvents
import com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl.NoPrimaryEffect
import com.akkaserverless.javasdk.testkit.EventSourcedResult
import com.akkaserverless.javasdk.testkit.ServiceCallDetails
import com.akkaserverless.javasdk.testkit.impl.EventSourcedResultImpl.eventsOf

import java.util.Collections
import java.util.{ List => JList }
import scala.jdk.CollectionConverters._

/**
 * INTERNAL API
 */
private[akkaserverless] object EventSourcedResultImpl {
  def eventsOf(effect: EventSourcedEntity.Effect[_]): JList[Any] = {
    effect match {
      case ei: EventSourcedEntityEffectImpl[_] =>
        ei.primaryEffect match {
          case ee: EmitEvents          => ee.event.toList.asJava
          case _: NoPrimaryEffect.type => Collections.emptyList()
        }
    }
  }
}

/**
 * INTERNAL API
 */
private[akkaserverless] final class EventSourcedResultImpl[R, S](effect: EventSourcedEntityEffectImpl[S], state: S)
    extends EventSourcedResult[R] {

  def this(effect: EventSourcedEntity.Effect[S], state: S) =
    this(effect.asInstanceOf[EventSourcedEntityEffectImpl[S]], state)

  private lazy val eventsIterator = getAllEvents().iterator

  private val appliedSecondaryEffect: SecondaryEffectImpl = effect match {
    case ese: EventSourcedEntityEffectImpl[_] => ese.secondaryEffect(state)
  }

  private def secondaryEffectName: String = appliedSecondaryEffect match {
    case _: MessageReplyImpl[_] => "reply"
    case _: ForwardReplyImpl[_] => "forward"
    case _: ErrorReplyImpl[_]   => "error"
    case _: NoReply[_]          => "noReply"
    case NoSecondaryEffectImpl  => "no effect" // this should never happen
  }

  /** All emitted events. */
  override def getAllEvents(): java.util.List[Any] = eventsOf(effect)

  override def isReply: Boolean = appliedSecondaryEffect.isInstanceOf[MessageReplyImpl[_]]

  def getReply: R = appliedSecondaryEffect match {
    case MessageReplyImpl(reply, _, _) => reply.asInstanceOf[R]
    case _                             => throw new IllegalStateException(s"The effect was not a reply but [$secondaryEffectName]")
  }

  override def isForward: Boolean = appliedSecondaryEffect.isInstanceOf[ForwardReplyImpl[_]]

  override def getForward: ServiceCallDetails[R] = appliedSecondaryEffect match {
    case ForwardReplyImpl(serviceCall: TestKitServiceCallFactory.TestKitServiceCall[R @unchecked], _) => serviceCall
    case _                                                                                            => throw new IllegalStateException(s"The effect was not a forward but [$secondaryEffectName]")
  }

  override def isError: Boolean = appliedSecondaryEffect.isInstanceOf[ErrorReplyImpl[_]]

  override def getError: String = appliedSecondaryEffect match {
    case ErrorReplyImpl(description, _) => description
    case _                              => throw new IllegalStateException(s"The effect was not an error but [$secondaryEffectName]")
  }

  override def isNoReply: Boolean = appliedSecondaryEffect.isInstanceOf[NoReply[_]]

  override def getUpdatedState: AnyRef = state.asInstanceOf[AnyRef]

  override def didEmitEvents(): Boolean = !getAllEvents().isEmpty

  override def getNextEventOfType[E](expectedClass: Class[E]): E =
    if (!eventsIterator.hasNext) throw new NoSuchElementException("No more events found")
    else {
      @SuppressWarnings(Array("unchecked")) val next = eventsIterator.next
      if (expectedClass.isInstance(next)) next.asInstanceOf[E]
      else
        throw new NoSuchElementException(
          "expected event type [" + expectedClass.getName + "] but found [" + next.getClass.getName + "]")
    }

}
