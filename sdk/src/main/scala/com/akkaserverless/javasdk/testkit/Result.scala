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

package com.akkaserverless.javasdk.testkit

import scala.reflect.ClassTag
import java.util.NoSuchElementException

class Result[Reply](val reply: Reply, events: collection.mutable.Buffer[Any]) {

  final val eventsIterator = events.iterator

  def getAllEvents: List[Any] = events.to(List)

  def getNextEvent: Any = eventsIterator.next

  def getReply: Reply = reply

  def getNextEventOfType[E](expectedClass: Class[E]): E = {
    eventOf(ClassTag[E](expectedClass))
  }

  private def eventOf[E: ClassTag]: E = {
    eventsIterator.next match {
      case e: E => e
      case other =>
        val expectedClass = implicitly[ClassTag[E]].runtimeClass
        throw new NoSuchElementException(
          s"expected [$expectedClass] " +
          s"but found ${other.getClass}"
        )
    }
  }
}
