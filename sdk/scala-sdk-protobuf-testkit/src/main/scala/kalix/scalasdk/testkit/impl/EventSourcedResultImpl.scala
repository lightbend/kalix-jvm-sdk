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

import scala.collection.immutable.Seq
import kalix.javasdk.impl.effect._
import kalix.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl.{ EmitEvents, NoPrimaryEffect }
import kalix.scalasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl
import kalix.scalasdk.testkit.{ DeferredCallDetails, EventSourcedResult }
import kalix.scalasdk.eventsourcedentity.EventSourcedEntity
import io.grpc.Status

import scala.reflect.ClassTag

/**
 * INTERNAL API Used by the generated testkit
 */
final class EventSourcedResultImpl[R, S](
    primaryEffect: EventSourcedEntityEffectImpl[R, S],
    state: S,
    secondaryEffect: SecondaryEffectImpl)
    extends EventSourcedResult[R] {
  import EventSourcedResultImpl._

  def this(effect: EventSourcedEntity.Effect[R], state: S, secondaryEffect: SecondaryEffectImpl) =
    this(effect.asInstanceOf[EventSourcedEntityEffectImpl[R, S]], state, secondaryEffect)

  private lazy val eventsIterator = events.iterator

  override def events: Seq[Any] = eventsOf(primaryEffect)

  override def isReply: Boolean =
    secondaryEffect.isInstanceOf[MessageReplyImpl[_]]

  private def secondaryEffectName: String =
    EffectUtils.nameFor(secondaryEffect)

  override def reply: R =
    secondaryEffect match {
      case reply: MessageReplyImpl[R @unchecked] => reply.message
      case _ => throw new IllegalStateException(s"The effect was not a reply but [$secondaryEffectName]")
    }

  override def isForward: Boolean =
    secondaryEffect.isInstanceOf[ForwardReplyImpl[_]]

  override def forwardedTo: DeferredCallDetails[_, R] =
    EffectUtils.forwardDetailsFor(secondaryEffect)

  override def isError: Boolean =
    secondaryEffect.isInstanceOf[ErrorReplyImpl[_]]

  override def errorDescription: String =
    secondaryEffect match {
      case error: ErrorReplyImpl[_] => error.description
      case _ => throw new IllegalStateException(s"The effect was not an error but [$secondaryEffectName]")
    }

  override def errorStatusCode: Status.Code =
    secondaryEffect match {
      case error: ErrorReplyImpl[_] => error.status.getOrElse(Status.Code.UNKNOWN)
      case _ => throw new IllegalStateException(s"The effect was not an error but [$secondaryEffectName]")
    }

  override def updatedState: S = state

  override def didEmitEvents: Boolean = events.nonEmpty

  override def nextEvent[E](implicit expectedClass: ClassTag[E]): E =
    if (!eventsIterator.hasNext) throw new NoSuchElementException("No more events found")
    else {
      @SuppressWarnings(Array("unchecked")) val next = eventsIterator.next()
      if (expectedClass.runtimeClass.isInstance(next)) next.asInstanceOf[E]
      else
        throw new NoSuchElementException(
          "expected event type [" + expectedClass.runtimeClass.getName + "] but found [" + next.getClass.getName + "]")
    }

  override def sideEffects: Seq[DeferredCallDetails[_, _]] =
    EffectUtils.toDeferredCallDetails(secondaryEffect.sideEffects)

}

/**
 * INTERNAL API
 */
object EventSourcedResultImpl {

  def eventsOf(effect: EventSourcedEntity.Effect[_]): Seq[Any] = {
    effect match {
      case ei: EventSourcedEntityEffectImpl[_, _] =>
        ei.javasdkEffect.primaryEffect match {
          case ee: EmitEvents[_]       => ee.event.toList
          case _: NoPrimaryEffect.type => Nil
        }
    }
  }

  def secondaryEffectOf[S](effect: EventSourcedEntity.Effect[_], state: S): SecondaryEffectImpl = {
    effect match {
      case ei: EventSourcedEntityEffectImpl[_, S @unchecked] =>
        ei.javasdkEffect.secondaryEffect(state)
    }
  }
}
