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

package kalix.springsdk.impl.eventsourcedentity

import kalix.springsdk.annotations.EventHandler

import java.lang.reflect.{ Method, Modifier, ParameterizedType, Type }

object EventHandlersExtractor {
  def handlersFrom(entityClass: Class[_]): EventSourceEntityHandlers = {

    val annotatedHandlers = entityClass.getDeclaredMethods
      .filter(_.getAnnotation(classOf[EventHandler]) != null)

    val expectedReturnType: Type =
      entityClass.getGenericSuperclass.asInstanceOf[ParameterizedType].getActualTypeArguments.apply(0)
    val invalidHandlers = annotatedHandlers
      .filter((m: Method) =>
        m.getParameterCount != 1 || !Modifier.isPublic(m.getModifiers) || (expectedReturnType ne m.getReturnType))
      .map(_.getName)

    if (!invalidHandlers.isEmpty)
      throw new IllegalArgumentException(
        "Event Sourced Entity [" + entityClass.getName + "] has '@EventHandler' methods " + invalidHandlers
          .mkString("(", ", ", ")") +
        " with a wrong signature: it must public, with exactly 1 unique parameter and return type '" + expectedReturnType.getTypeName + "'")

    def eventTypeExtractor: Method => Class[_] = (mt: Method) => mt.getParameterTypes.apply(0)

    val eventTypeGrouped = annotatedHandlers
      .groupBy(eventTypeExtractor)

    eventTypeGrouped.filter(_._2.length > 1).foreach { case (k, v) =>
      throw new IllegalArgumentException(
        "Event Sourced Entity [" + v(0).getDeclaringClass.getName + "] " + "cannot have duplicate event handlers (" + v(
          0).getName + ", " + v(1).getName + ") " +
        "for the same event type: " + v(0).getParameterTypes.apply(0).getName)
    }

    EventSourceEntityHandlers(eventTypeGrouped.transform { case (_, v) => v(0) })
  }
}

private[springsdk] final case class EventSourceEntityHandlers private (handlers: Map[Class[_], Method])
