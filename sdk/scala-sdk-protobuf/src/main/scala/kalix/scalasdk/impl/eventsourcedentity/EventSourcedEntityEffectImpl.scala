/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.impl.eventsourcedentity

import scala.jdk.CollectionConverters._
import scala.compat.java8.FunctionConverters._
import kalix.javasdk
import kalix.scalasdk.{ DeferredCall, Metadata, SideEffect }
import kalix.scalasdk.eventsourcedentity.EventSourcedEntity
import kalix.scalasdk.impl.ScalaDeferredCallAdapter
import kalix.scalasdk.impl.ScalaSideEffectAdapter
import io.grpc.Status

private[scalasdk] object EventSourcedEntityEffectImpl {
  def apply[R, S](): EventSourcedEntityEffectImpl[R, S] = EventSourcedEntityEffectImpl(
    new javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl[S, Any]())
}

private[scalasdk] final case class EventSourcedEntityEffectImpl[R, S](
    javasdkEffect: javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl[S, Any])
    extends EventSourcedEntity.Effect.Builder[S]
    with EventSourcedEntity.Effect.OnSuccessBuilder[S]
    with EventSourcedEntity.Effect[R] {

  def emitEvent(event: Object): EventSourcedEntity.Effect.OnSuccessBuilder[S] = EventSourcedEntityEffectImpl(
    javasdkEffect.emitEvent(event))

  def emitEvents(event: List[_]): EventSourcedEntity.Effect.OnSuccessBuilder[S] =
    EventSourcedEntityEffectImpl(javasdkEffect.emitEvents(event.asJava))

  def deleteEntity(): EventSourcedEntity.Effect.OnSuccessBuilder[S] = EventSourcedEntityEffectImpl(
    javasdkEffect.deleteEntity())

  def error[T](description: String): EventSourcedEntity.Effect[T] =
    EventSourcedEntityEffectImpl(javasdkEffect.error[T](description))

  def error[T](description: String, statusCode: Status.Code): EventSourcedEntity.Effect[T] =
    EventSourcedEntityEffectImpl(javasdkEffect.error[T](description, statusCode))

  def forward[T](deferredCall: DeferredCall[_, T]): EventSourcedEntity.Effect[T] =
    deferredCall match {
      case ScalaDeferredCallAdapter(jdc) => EventSourcedEntityEffectImpl(javasdkEffect.forward(jdc))
    }

  def reply[T](message: T, metadata: Metadata): EventSourcedEntity.Effect[T] = EventSourcedEntityEffectImpl(
    javasdkEffect.reply(message, metadata.impl))

  def reply[T](message: T): EventSourcedEntity.Effect[T] = EventSourcedEntityEffectImpl(javasdkEffect.reply(message))

  def addSideEffects(sideEffects: Seq[SideEffect]): EventSourcedEntity.Effect[R] =
    EventSourcedEntityEffectImpl(javasdkEffect.addSideEffects(sideEffects.map { case ScalaSideEffectAdapter(se) =>
      se
    }.asJavaCollection))

  def thenAddSideEffect(sideEffect: S => SideEffect): EventSourcedEntity.Effect.OnSuccessBuilder[S] =
    EventSourcedEntityEffectImpl(javasdkEffect.thenAddSideEffect { s =>
      sideEffect(s) match { case ScalaSideEffectAdapter(s) => s }
    })

  def thenForward[T](serviceCall: S => DeferredCall[_, T]): EventSourcedEntity.Effect[T] = EventSourcedEntityEffectImpl(
    javasdkEffect.thenForward[T] { s =>
      val scalaDeferredCall = serviceCall(s)
      scalaDeferredCall match {
        case ScalaDeferredCallAdapter(javaSdkDeferredCall) => javaSdkDeferredCall
      }
    })

  def thenReply[T](replyMessage: S => T, metadata: Metadata): EventSourcedEntity.Effect[T] =
    EventSourcedEntityEffectImpl(javasdkEffect.thenReply(replyMessage.asJava, metadata.impl))

  def thenReply[T](replyMessage: S => T): EventSourcedEntity.Effect[T] =
    EventSourcedEntityEffectImpl(javasdkEffect.thenReply { s => replyMessage(s) })
}
