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

package com.akkaserverless.scalasdk.impl.eventsourcedentity

import scala.jdk.CollectionConverters._
import scala.compat.java8.FunctionConverters._
import com.akkaserverless.javasdk
import com.akkaserverless.scalasdk.Metadata
import com.akkaserverless.scalasdk.ServiceCall
import com.akkaserverless.scalasdk.SideEffect
import com.akkaserverless.scalasdk.impl.JavaServiceCallAdapter
import com.akkaserverless.scalasdk.impl.JavaSideEffectAdapter
import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity

private[scalasdk] object EventSourcedEntityEffectImpl {
  def apply[S](): EventSourcedEntityEffectImpl[S] = EventSourcedEntityEffectImpl(
    new javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl[S]())
}

private[scalasdk] final case class EventSourcedEntityEffectImpl[S](
    javasdkEffect: javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl[S])
    extends EventSourcedEntity.Effect.Builder[S]
    with EventSourcedEntity.Effect.OnSuccessBuilder[S]
    with EventSourcedEntity.Effect[S] {

  def emitEvent(event: Object): EventSourcedEntity.Effect.OnSuccessBuilder[S] = EventSourcedEntityEffectImpl(
    javasdkEffect.emitEvent(event))

  def emitEvents(event: List[_]): EventSourcedEntity.Effect.OnSuccessBuilder[S] =
    EventSourcedEntityEffectImpl(javasdkEffect.emitEvents(event.asJava))

  def error[T](description: String): EventSourcedEntity.Effect[T] = EventSourcedEntityEffectImpl(
    javasdkEffect.error(description))

  def forward[T](serviceCall: ServiceCall): EventSourcedEntity.Effect[T] = EventSourcedEntityEffectImpl(
    javasdkEffect.forward(JavaServiceCallAdapter(serviceCall)))

  def noReply[T]: EventSourcedEntity.Effect[T] = EventSourcedEntityEffectImpl(javasdkEffect.noReply())

  def reply[T](message: T, metadata: Metadata): EventSourcedEntity.Effect[T] = EventSourcedEntityEffectImpl(
    javasdkEffect.reply(message, metadata.impl))

  def reply[T](message: T): EventSourcedEntity.Effect[T] = EventSourcedEntityEffectImpl(javasdkEffect.reply(message))

  def addSideEffects(sideEffects: Seq[SideEffect]): EventSourcedEntity.Effect[S] =
    EventSourcedEntityEffectImpl(
      javasdkEffect.addSideEffects(
        sideEffects.map(se => JavaSideEffectAdapter(se).asInstanceOf[javasdk.SideEffect]).asJavaCollection))

  def thenAddSideEffect(sideEffect: S => SideEffect): EventSourcedEntity.Effect.OnSuccessBuilder[S] =
    EventSourcedEntityEffectImpl(javasdkEffect.thenAddSideEffect { s => JavaSideEffectAdapter(sideEffect(s)) })

  def thenForward[T](serviceCall: S => ServiceCall): EventSourcedEntity.Effect[T] = EventSourcedEntityEffectImpl(
    javasdkEffect.thenForward { s => JavaServiceCallAdapter(serviceCall(s)) })

  def thenNoReply[T]: EventSourcedEntity.Effect[T] = EventSourcedEntityEffectImpl(javasdkEffect.thenNoReply())

  def thenReply[T](replyMessage: S => T, metadata: Metadata): EventSourcedEntity.Effect[T] =
    EventSourcedEntityEffectImpl(javasdkEffect.thenReply(replyMessage.asJava, metadata.impl))

  def thenReply[T](replyMessage: S => T): EventSourcedEntity.Effect[T] =
    EventSourcedEntityEffectImpl(javasdkEffect.thenReply { s => replyMessage(s) })
}
