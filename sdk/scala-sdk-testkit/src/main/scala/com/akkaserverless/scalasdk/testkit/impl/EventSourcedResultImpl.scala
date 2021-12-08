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

import scala.collection.immutable.Seq
import com.akkaserverless.javasdk.impl.effect._
import com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl.{ EmitEvents, NoPrimaryEffect }
import com.akkaserverless.scalasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl
import com.akkaserverless.scalasdk.testkit.{ DeferredCallDetails, EventSourcedResult }
import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity
import com.akkaserverless.javasdk.SideEffect
import com.akkaserverless.scalasdk.DeferredCall

import com.akkaserverless.javasdk.impl.DeferredCallImpl

import scala.reflect.ClassTag

/**
 * INTERNAL API Used by the generated testkit
 */
final class EventSourcedResultImpl[R, S](
    effect: EventSourcedEntityEffectImpl[R, S],
    state: S,
    secondaryEffect: SecondaryEffectImpl)
    extends EventSourcedResult[R] {
  import EventSourcedResultImpl._

  def this(effect: EventSourcedEntity.Effect[R], state: S, secondaryEffect: SecondaryEffectImpl) =
    this(effect.asInstanceOf[EventSourcedEntityEffectImpl[R, S]], state, secondaryEffect)

  //DELETE before after codegen generates the proper TestKit
  // only for compatibility while WIP
  def this(effect: EventSourcedEntity.Effect[R], state: S) =
    this(effect.asInstanceOf[EventSourcedEntityEffectImpl[R, S]], state, NoSecondaryEffectImpl)

  private lazy val eventsIterator = events.iterator

  override def events: Seq[Any] = eventsOf(effect)

  override def isReply: Boolean =
    effect.javasdkEffect.secondaryEffect(state).isInstanceOf[MessageReplyImpl[_]]

  private def secondaryEffectName: String =
    effect.javasdkEffect.secondaryEffect(state) match {
      case _: MessageReplyImpl[_] => "reply"
      case _: ForwardReplyImpl[_] => "forward"
      case _: ErrorReplyImpl[_]   => "error"
      case _: NoReply[_]          => "noReply"
      case NoSecondaryEffectImpl  => "no effect" // this should never happen
    }

  override def reply: R =
    effect.javasdkEffect.secondaryEffect(state) match {
      case reply: MessageReplyImpl[R @unchecked] => reply.message
      case _ => throw new IllegalStateException(s"The effect was not a reply but [$secondaryEffectName]")
    }

  override def isForward: Boolean =
    effect.javasdkEffect.secondaryEffect(state).isInstanceOf[ForwardReplyImpl[_]]

  override def forwardedTo: DeferredCallDetails[_, R] =
    ??? // FIXME #587
//    effect.javasdkEffect.secondaryEffect match {
//    case reply: ForwardReplyImpl[R @unchecked] =>
//      reply.serviceCall match {
//        case t: TestKitServiceCallFactory.TestKitServiceCall[R @unchecked] =>
//          t
//        case surprise =>
//          throw new IllegalStateException(s"Unexpected type of service call in testkit: ${surprise.getClass.getName}")
//      }
//    case _ => throw new IllegalStateException(s"The effect was not a forward but [$secondaryEffectName]")
//  }

  override def isError: Boolean =
    effect.javasdkEffect.secondaryEffect(state).isInstanceOf[ErrorReplyImpl[_]]

  override def errorDescription: String =
    effect.javasdkEffect.secondaryEffect(state) match {
      case error: ErrorReplyImpl[_] => error.description
      case _ => throw new IllegalStateException(s"The effect was not an error but [$secondaryEffectName]")
    }

  override def isNoReply: Boolean =
    effect.javasdkEffect.secondaryEffect(state).isInstanceOf[NoReply[_]]

  override def updatedState: S = state

  override def didEmitEvents: Boolean = events.nonEmpty

  override def nextEvent[E](implicit expectedClass: ClassTag[E]): E =
    if (!eventsIterator.hasNext) throw new NoSuchElementException("No more events found")
    else {
      @SuppressWarnings(Array("unchecked")) val next = eventsIterator.next
      if (expectedClass.runtimeClass.isInstance(next)) next.asInstanceOf[E]
      else
        throw new NoSuchElementException(
          "expected event type [" + expectedClass.runtimeClass.getName + "] but found [" + next.getClass.getName + "]")
    }

  private def extractServices(sideEffects: Vector[SideEffect]): Seq[DeferredCallDetails[_, _]] = {
    sideEffects.map { sideEffect =>
      TestKitDeferredCall(sideEffect.call.asInstanceOf[DeferredCallImpl[_, _]])
    }
  }

  override def sideEffects(): Seq[DeferredCallDetails[_, _]] = secondaryEffect match {
    case MessageReplyImpl(_, _, sideEffects) => extractServices(sideEffects)
    case ForwardReplyImpl(_, sideEffects)    => extractServices(sideEffects)
    case ErrorReplyImpl(_, sideEffects)      => extractServices(sideEffects)
    case NoReply(sideEffects)                => extractServices(sideEffects)
    case NoSecondaryEffectImpl               => Nil // this should never happen
  }

}

/**
 * INTERNAL API
 */
object EventSourcedResultImpl {
  def eventsOf(effect: EventSourcedEntity.Effect[_]): Seq[Any] = {
    effect match {
      case ei: EventSourcedEntityEffectImpl[_, _] =>
        ei.javasdkEffect.primaryEffect match {
          case ee: EmitEvents          => ee.event.toList
          case _: NoPrimaryEffect.type => Nil
        }
    }
  }

  def secondaryEffectOf[S](effect: EventSourcedEntity.Effect[_], state: S): SecondaryEffectImpl = {
    effect match {
      case ei: EventSourcedEntityEffectImpl[_, S] =>
        ei.javasdkEffect.secondaryEffect(state)
    }
  }
}
