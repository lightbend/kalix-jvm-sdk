/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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
