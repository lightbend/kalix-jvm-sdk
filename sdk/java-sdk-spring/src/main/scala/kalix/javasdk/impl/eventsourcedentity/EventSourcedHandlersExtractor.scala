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

package kalix.javasdk.impl.eventsourcedentity

import kalix.javasdk.annotations.EventHandler
import kalix.javasdk.impl.JsonMessageCodec
import kalix.javasdk.impl.MethodInvoker
import kalix.javasdk.impl.reflection.ParameterExtractors

object EventSourcedHandlersExtractor {
  def handlersFrom(entityClass: Class[_], messageCodec: JsonMessageCodec): Map[String, MethodInvoker] = {

    val annotatedHandlers = entityClass.getDeclaredMethods
      .filter(_.getAnnotation(classOf[EventHandler]) != null)
      .toList

    if (annotatedHandlers.size == 1 && annotatedHandlers.head.getParameterTypes.head.isSealed) {
      val singleHandler = annotatedHandlers.head
      val eventClass = singleHandler.getParameterTypes.head
      eventClass.getPermittedSubclasses.toList.flatMap { subClass =>
        val invoker = MethodInvoker(singleHandler, ParameterExtractors.AnyBodyExtractor[AnyRef](subClass))
        //in case of schema evolution more types can point to the same invoker
        messageCodec.typeUrlsFor(subClass).map(typeUrl => typeUrl -> invoker)
      }.toMap
    } else {
      annotatedHandlers.flatMap { method =>
        val eventClass = method.getParameterTypes.head
        val invoker = MethodInvoker(method, ParameterExtractors.AnyBodyExtractor[AnyRef](eventClass))
        //in case of schema evolution more types can point to the same invoker
        messageCodec.typeUrlsFor(eventClass).map(typeUrl => typeUrl -> invoker)
      }.toMap
    }
  }
}
