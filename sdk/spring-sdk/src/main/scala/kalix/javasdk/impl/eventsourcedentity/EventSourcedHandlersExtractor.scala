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

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType

import kalix.javasdk.annotations.EventHandler
import kalix.javasdk.impl.MethodInvoker
import kalix.javasdk.impl.JsonMessageCodec
import kalix.javasdk.impl.reflection.ParameterExtractors

object EventSourcedHandlersExtractor {
  def handlersFrom(entityClass: Class[_], messageCodec: JsonMessageCodec): EventSourceEntityHandlers = {

    val annotatedHandlers = entityClass.getDeclaredMethods
      .filter(_.getAnnotation(classOf[EventHandler]) != null)
      .toList

    // the type parameter from the entity defines the return type of each event handler
    val returnType =
      entityClass.getGenericSuperclass
        .asInstanceOf[ParameterizedType]
        .getActualTypeArguments
        .head
        .asInstanceOf[Class[_]]
    val (invalidHandlers, validSignatureHandlers) = annotatedHandlers.partition((m: Method) =>
      m.getParameterCount != 1 || !Modifier.isPublic(m.getModifiers) || (returnType != m.getReturnType))

    def eventTypeExtractor: Method => Class[_] = (mt: Method) => mt.getParameterTypes.head
    val eventTypeGrouped = validSignatureHandlers
      .groupBy(eventTypeExtractor)

    val (duplicatedEventTypes, validHandlers) = eventTypeGrouped.partition(_._2.length > 1)

    val errorsForSignatures =
      if (invalidHandlers.isEmpty) List.empty
      else
        List(
          HandlerValidationError(
            invalidHandlers,
            "must be public, with exactly one parameter and return type '" + returnType.getTypeName + "'"))
    val errorsForDuplicates =
      for (elem <- duplicatedEventTypes)
        yield HandlerValidationError(
          elem._2,
          "cannot have duplicate event handlers for the same event type: '" + elem._1.getName + "'")

    EventSourceEntityHandlers(
      handlers = validHandlers.map { case (classType, methods) =>
        messageCodec.typeUrlFor(classType) -> MethodInvoker(
          methods.head,
          ParameterExtractors.AnyBodyExtractor[AnyRef](classType))
      },
      errors = errorsForSignatures ++ errorsForDuplicates.toList)
  }
}

private[kalix] final case class EventSourceEntityHandlers private (
    handlers: Map[String, MethodInvoker],
    errors: List[HandlerValidationError])

private[kalix] final case class HandlerValidationError(methods: List[Method], description: String) {
  override def toString: String =
    s"ValidationError(reason='$description', offendingMethods=${methods.map(_.getName)}"
}
