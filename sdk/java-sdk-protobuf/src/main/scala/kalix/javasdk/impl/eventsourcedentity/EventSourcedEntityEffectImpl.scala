/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.eventsourcedentity

import io.grpc.Status
import kalix.javasdk.StatusCode.ErrorCode
import kalix.javasdk.eventsourcedentity.EventSourcedEntity.Effect
import kalix.javasdk.eventsourcedentity.EventSourcedEntity.Effect.Builder
import kalix.javasdk.eventsourcedentity.EventSourcedEntity.Effect.OnSuccessBuilder
import kalix.javasdk.impl.StatusCodeConverter
import kalix.javasdk.impl.effect.ErrorReplyImpl
import kalix.javasdk.impl.effect.ForwardReplyImpl
import kalix.javasdk.impl.effect.MessageReplyImpl
import kalix.javasdk.impl.effect.NoSecondaryEffectImpl
import kalix.javasdk.impl.effect.SecondaryEffectImpl
import kalix.javasdk.DeferredCall
import kalix.javasdk.Metadata
import kalix.javasdk.SideEffect

import java.util
import java.util.function.{ Function => JFunction }
import scala.jdk.CollectionConverters._

object EventSourcedEntityEffectImpl {
  sealed trait PrimaryEffectImpl
  final case class EmitEvents[E](event: Iterable[E], deleteEntity: Boolean = false) extends PrimaryEffectImpl
  case object NoPrimaryEffect extends PrimaryEffectImpl
}

class EventSourcedEntityEffectImpl[S, E] extends Builder[S, E] with OnSuccessBuilder[S] with Effect[S] {
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

  override def emitEvent(event: E): EventSourcedEntityEffectImpl[S, E] = {
    if (event.isInstanceOf[Iterable[_]] || event.isInstanceOf[java.lang.Iterable[_]]) {
      throw new IllegalStateException(
        s"You are trying to emit collection (${event.getClass}) of events. Use `emitEvents` method instead.")
    } else {
      _primaryEffect = EmitEvents(Vector(event))
      this
    }
  }

  override def emitEvents(events: util.List[_ <: E]): EventSourcedEntityEffectImpl[S, E] = {
    _primaryEffect = EmitEvents(events.asScala)
    this
  }

  override def deleteEntity(): EventSourcedEntityEffectImpl[S, E] = {
    _primaryEffect = _primaryEffect match {
      case NoPrimaryEffect           => EmitEvents[E](Vector.empty, deleteEntity = true)
      case emitEvents: EmitEvents[_] => emitEvents.copy(deleteEntity = true)
    }
    this
  }

  override def reply[T](message: T): EventSourcedEntityEffectImpl[T, E] =
    reply(message, Metadata.EMPTY)

  override def reply[T](message: T, metadata: Metadata): EventSourcedEntityEffectImpl[T, E] = {
    _secondaryEffect = MessageReplyImpl(message, metadata, _secondaryEffect.sideEffects)
    this.asInstanceOf[EventSourcedEntityEffectImpl[T, E]]
  }

  override def forward[T](serviceCall: DeferredCall[_, T]): EventSourcedEntityEffectImpl[T, E] = {
    _secondaryEffect = ForwardReplyImpl(serviceCall, _secondaryEffect.sideEffects)
    this.asInstanceOf[EventSourcedEntityEffectImpl[T, E]]
  }

  override def error[T](description: String): EventSourcedEntityEffectImpl[T, E] = {
    _secondaryEffect = ErrorReplyImpl(description, None, _secondaryEffect.sideEffects)
    this.asInstanceOf[EventSourcedEntityEffectImpl[T, E]]
  }

  override def error[T](description: String, grpcErrorCode: Status.Code): EventSourcedEntityEffectImpl[T, E] = {
    if (grpcErrorCode.toStatus.isOk) throw new IllegalArgumentException("Cannot fail with a success status")
    _secondaryEffect = ErrorReplyImpl(description, Some(grpcErrorCode), _secondaryEffect.sideEffects)
    this.asInstanceOf[EventSourcedEntityEffectImpl[T, E]]
  }

  override def error[T](description: String, httpErrorCode: ErrorCode): EventSourcedEntityEffectImpl[T, E] = {
    _secondaryEffect =
      ErrorReplyImpl(description, Some(StatusCodeConverter.toGrpcCode(httpErrorCode)), _secondaryEffect.sideEffects)
    this.asInstanceOf[EventSourcedEntityEffectImpl[T, E]]
  }

  override def thenReply[T](replyMessage: JFunction[S, T]): EventSourcedEntityEffectImpl[T, E] =
    thenReply(replyMessage, Metadata.EMPTY)

  override def thenReply[T](replyMessage: JFunction[S, T], metadata: Metadata): EventSourcedEntityEffectImpl[T, E] = {
    _functionSecondaryEffect = state => MessageReplyImpl(replyMessage.apply(state), metadata, Vector.empty)
    this.asInstanceOf[EventSourcedEntityEffectImpl[T, E]]
  }

  override def thenForward[T](serviceCall: JFunction[S, DeferredCall[_, T]]): EventSourcedEntityEffectImpl[T, E] = {
    _functionSecondaryEffect = state => ForwardReplyImpl(serviceCall.apply(state), Vector.empty)
    this.asInstanceOf[EventSourcedEntityEffectImpl[T, E]]
  }

  override def thenAddSideEffect(sideEffect: JFunction[S, SideEffect]): EventSourcedEntityEffectImpl[S, E] = {
    _functionSideEffects :+= sideEffect
    this
  }

  override def addSideEffects(sideEffects: util.Collection[SideEffect]): EventSourcedEntityEffectImpl[S, E] = {
    _secondaryEffect = _secondaryEffect.addSideEffects(sideEffects.asScala)
    this
  }

  override def addSideEffects(sideEffects: SideEffect*): EventSourcedEntityEffectImpl[S, E] = {
    _secondaryEffect = _secondaryEffect.addSideEffects(sideEffects)
    this
  }
}
