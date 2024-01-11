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

package kalix.javasdk.testkit.impl

import kalix.javasdk.SideEffect
import kalix.javasdk.eventsourcedentity.EventSourcedEntity
import kalix.javasdk.impl.GrpcDeferredCall
import kalix.javasdk.impl.effect.ErrorReplyImpl
import kalix.javasdk.impl.effect.ForwardReplyImpl
import kalix.javasdk.impl.effect.MessageReplyImpl
import kalix.javasdk.impl.effect.NoSecondaryEffectImpl
import kalix.javasdk.impl.effect.SecondaryEffectImpl
import kalix.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl
import kalix.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl.EmitEvents
import kalix.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl.NoPrimaryEffect
import kalix.javasdk.testkit.DeferredCallDetails
import kalix.javasdk.testkit.EventSourcedResult
import kalix.javasdk.testkit.impl.EventSourcedResultImpl.eventsOf
import java.util.Collections
import java.util.{ List => JList }

import io.grpc.Status

import scala.jdk.CollectionConverters._

/**
 * INTERNAL API
 */
private[kalix] object EventSourcedResultImpl {
  def eventsOf[E](effect: EventSourcedEntity.Effect[_]): JList[E] = {
    effect match {
      case ei: EventSourcedEntityEffectImpl[_, E @unchecked] =>
        ei.primaryEffect match {
          case ee: EmitEvents[E @unchecked] => ee.event.toList.asJava
          case _: NoPrimaryEffect.type      => Collections.emptyList()
        }
    }
  }

  def secondaryEffectOf[S](effect: EventSourcedEntity.Effect[_], state: S): SecondaryEffectImpl = {
    effect match {
      case ei: EventSourcedEntityEffectImpl[S @unchecked, _] =>
        ei.secondaryEffect(state)
    }
  }

  private def toDeferredCallDetails(sideEffects: Vector[SideEffect]): JList[DeferredCallDetails[_, _]] = {
    sideEffects
      .map { sideEffect =>
        TestKitDeferredCall(sideEffect.call.asInstanceOf[GrpcDeferredCall[_, _]])
          .asInstanceOf[DeferredCallDetails[_, _]] // java List is invariant in type
      }
      .toList
      .asJava
  }

}

/**
 * INTERNAL API
 */
private[kalix] final class EventSourcedResultImpl[R, S, E](
    effect: EventSourcedEntityEffectImpl[S, E],
    state: S,
    secondaryEffect: SecondaryEffectImpl)
    extends EventSourcedResult[R] {
  import EventSourcedResultImpl._

  def this(effect: EventSourcedEntity.Effect[R], state: S, secondaryEffect: SecondaryEffectImpl) =
    this(effect.asInstanceOf[EventSourcedEntityEffectImpl[S, E]], state, secondaryEffect)

  private lazy val eventsIterator = getAllEvents().iterator

  private def secondaryEffectName: String = secondaryEffect match {
    case _: MessageReplyImpl[_]   => "reply"
    case _: ForwardReplyImpl[_]   => "forward"
    case _: ErrorReplyImpl[_]     => "error"
    case _: NoSecondaryEffectImpl => "no effect" // this should never happen
  }

  /** All emitted events. */
  override def getAllEvents: java.util.List[Any] = eventsOf(effect)

  override def isReply: Boolean = secondaryEffect.isInstanceOf[MessageReplyImpl[_]]

  def getReply: R = secondaryEffect match {
    case MessageReplyImpl(reply, _, _) => reply.asInstanceOf[R]
    case _ => throw new IllegalStateException(s"The effect was not a reply but [$secondaryEffectName]")
  }

  override def isForward: Boolean = secondaryEffect.isInstanceOf[ForwardReplyImpl[_]]

  override def getForward: DeferredCallDetails[_, R] = secondaryEffect match {
    case ForwardReplyImpl(deferredCall: GrpcDeferredCall[_, _], _) =>
      TestKitDeferredCall(deferredCall.asInstanceOf[GrpcDeferredCall[_, R]])
    case _ => throw new IllegalStateException(s"The effect was not a forward but [$secondaryEffectName]")
  }

  override def isError: Boolean = secondaryEffect.isInstanceOf[ErrorReplyImpl[_]]

  override def getError: String = secondaryEffect match {
    case ErrorReplyImpl(description, _, _) => description
    case _ => throw new IllegalStateException(s"The effect was not an error but [$secondaryEffectName]")
  }

  override def getErrorStatusCode: Status.Code = secondaryEffect match {
    case ErrorReplyImpl(_, status, _) => status.getOrElse(Status.Code.UNKNOWN)
    case _ => throw new IllegalStateException(s"The effect was not an error but [$secondaryEffectName]")
  }

  override def getUpdatedState: AnyRef = state.asInstanceOf[AnyRef]

  override def didEmitEvents(): Boolean = !getAllEvents().isEmpty

  override def getNextEventOfType[T](expectedClass: Class[T]): T =
    if (!eventsIterator.hasNext) throw new NoSuchElementException("No more events found")
    else {
      @SuppressWarnings(Array("unchecked")) val next = eventsIterator.next
      if (expectedClass.isInstance(next)) next.asInstanceOf[T]
      else
        throw new NoSuchElementException(
          "expected event type [" + expectedClass.getName + "] but found [" + next.getClass.getName + "]")
    }

  override def getSideEffects(): JList[DeferredCallDetails[_, _]] =
    toDeferredCallDetails(secondaryEffect.sideEffects)

}
