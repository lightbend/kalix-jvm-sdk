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

package kalix.javasdk.impl.eventsourcedentity

import kalix.javasdk.{ DeferredCall, Metadata, SideEffect }
import java.util
import java.util.function.{ Function => JFunction }

import scala.jdk.CollectionConverters._
import kalix.javasdk.Metadata
import kalix.javasdk.impl.effect.ErrorReplyImpl
import kalix.javasdk.impl.effect.ForwardReplyImpl
import kalix.javasdk.impl.effect.MessageReplyImpl
import kalix.javasdk.impl.effect.NoSecondaryEffectImpl
import kalix.javasdk.impl.effect.SecondaryEffectImpl
import kalix.javasdk.eventsourcedentity.EventSourcedEntity.Effect
import kalix.javasdk.eventsourcedentity.EventSourcedEntity.Effect.Builder
import kalix.javasdk.eventsourcedentity.EventSourcedEntity.Effect.OnSuccessBuilder
import io.grpc.Status

object EventSourcedEntityEffectImpl {
  sealed trait PrimaryEffectImpl
  final case class EmitEvents(event: Iterable[Any], deleteEntity: Boolean = false) extends PrimaryEffectImpl
  case object NoPrimaryEffect extends PrimaryEffectImpl
}

class EventSourcedEntityEffectImpl[S] extends Builder[S] with OnSuccessBuilder[S] with Effect[S] {
  import EventSourcedEntityEffectImpl._

  private var _primaryEffect: PrimaryEffectImpl = NoPrimaryEffect
  private var _secondaryEffect: SecondaryEffectImpl = NoSecondaryEffectImpl()

  private var _functionSecondaryEffect: Function[S, SecondaryEffectImpl] = _ => NoSecondaryEffectImpl()
  private var _functionSideEffects: Vector[JFunction[S, SideEffect]] = Vector.empty

  def primaryEffect: PrimaryEffectImpl = _primaryEffect

  def secondaryEffect(state: S): SecondaryEffectImpl = {
    var secondary =
      _functionSecondaryEffect(state) match {
        case NoSecondaryEffectImpl(_) => _secondaryEffect
        case newSecondary             => newSecondary.addSideEffects(_secondaryEffect.sideEffects)
      }
    if (_functionSideEffects.nonEmpty) {
      secondary = secondary.addSideEffects(_functionSideEffects.map(_.apply(state)))
    }
    secondary
  }

  override def emitEvent(event: Any): EventSourcedEntityEffectImpl[S] = {
    if (event.isInstanceOf[Iterable[_]] || event.isInstanceOf[java.lang.Iterable[_]]) {
      throw new IllegalStateException(
        s"You are trying to emit collection (${event.getClass}) of events. Use `emitEvents` method instead.")
    } else {
      _primaryEffect = EmitEvents(Vector(event))
      this
    }
  }

  override def emitEvents(events: util.List[_]): EventSourcedEntityEffectImpl[S] = {
    _primaryEffect = EmitEvents(events.asScala)
    this
  }

  override def deleteEntity(finalEvent: Any): EventSourcedEntityEffectImpl[S] = {
    if (finalEvent.isInstanceOf[Iterable[_]] || finalEvent.isInstanceOf[java.lang.Iterable[_]]) {
      throw new IllegalStateException(
        s"You are trying to emit collection (${finalEvent.getClass}) of events. " +
        s"Only one final event is supported when deleting the entity.")
    } else {
      _primaryEffect = EmitEvents(Vector(finalEvent), deleteEntity = true)
      this
    }
  }

  override def reply[T](message: T): EventSourcedEntityEffectImpl[T] =
    reply(message, Metadata.EMPTY)

  override def reply[T](message: T, metadata: Metadata): EventSourcedEntityEffectImpl[T] = {
    _secondaryEffect = MessageReplyImpl(message, metadata, _secondaryEffect.sideEffects)
    this.asInstanceOf[EventSourcedEntityEffectImpl[T]]
  }

  override def forward[T](serviceCall: DeferredCall[_, T]): EventSourcedEntityEffectImpl[T] = {
    _secondaryEffect = ForwardReplyImpl(serviceCall, _secondaryEffect.sideEffects)
    this.asInstanceOf[EventSourcedEntityEffectImpl[T]]
  }

  override def error[T](description: String): EventSourcedEntityEffectImpl[T] = {
    _secondaryEffect = ErrorReplyImpl(description, None, _secondaryEffect.sideEffects)
    this.asInstanceOf[EventSourcedEntityEffectImpl[T]]
  }

  override def error[T](description: String, statusCode: Status.Code): EventSourcedEntityEffectImpl[T] = {
    if (statusCode.toStatus.isOk) throw new IllegalArgumentException("Cannot fail with a success status")
    _secondaryEffect = ErrorReplyImpl(description, Some(statusCode), _secondaryEffect.sideEffects)
    this.asInstanceOf[EventSourcedEntityEffectImpl[T]]
  }

  override def thenReply[T](replyMessage: JFunction[S, T]): EventSourcedEntityEffectImpl[T] =
    thenReply(replyMessage, Metadata.EMPTY)

  override def thenReply[T](replyMessage: JFunction[S, T], metadata: Metadata): EventSourcedEntityEffectImpl[T] = {
    _functionSecondaryEffect = state => MessageReplyImpl(replyMessage.apply(state), metadata, Vector.empty)
    this.asInstanceOf[EventSourcedEntityEffectImpl[T]]
  }

  override def thenForward[T](serviceCall: JFunction[S, DeferredCall[_, T]]): EventSourcedEntityEffectImpl[T] = {
    _functionSecondaryEffect = state => ForwardReplyImpl(serviceCall.apply(state), Vector.empty)
    this.asInstanceOf[EventSourcedEntityEffectImpl[T]]
  }

  override def thenAddSideEffect(sideEffect: JFunction[S, SideEffect]): EventSourcedEntityEffectImpl[S] = {
    _functionSideEffects :+= sideEffect
    this
  }

  override def addSideEffects(sideEffects: util.Collection[SideEffect]): EventSourcedEntityEffectImpl[S] = {
    _secondaryEffect = _secondaryEffect.addSideEffects(sideEffects.asScala)
    this
  }

  override def addSideEffects(sideEffects: SideEffect*): EventSourcedEntityEffectImpl[S] = {
    _secondaryEffect = _secondaryEffect.addSideEffects(sideEffects)
    this
  }
}
