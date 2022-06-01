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

package kalix.springsdk.impl.action

import java.lang.reflect.Method

import akka.NotUsed
import akka.stream.javadsl.Source
import kalix.javasdk.action.Action
import kalix.javasdk.action.MessageEnvelope
import kalix.javasdk.impl.action.ActionRouter

class ActionReflectiveRouter[A <: Action](override val action: A) extends ActionRouter[A](action) {

  private val allHandlers: Map[String, Method] =
    // FIXME: names must be unique, overloading shouldn't be allowed,
    //  we should detect it here and fail-fast
    action.getClass.getDeclaredMethods.toList
      // handlers are all methods returning Effect
      .filter(_.getReturnType == classOf[Action.Effect[_]])
      // and with one single input param
      .filter(_.getParameters.length == 1)
      .map { javaMethod => (javaMethod.getName.capitalize, javaMethod) }
      .toMap

  private def methodLookup(commandName: String) =
    allHandlers.getOrElse(commandName, throw new RuntimeException(s"no matching method for '$commandName'"))

  override def handleUnary(commandName: String, message: MessageEnvelope[Any]): Action.Effect[_] =
    methodLookup(commandName)
      .invoke(action, message.payload())
      .asInstanceOf[Action.Effect[_]]

  override def handleStreamedOut(
      commandName: String,
      message: MessageEnvelope[Any]): Source[Action.Effect[_], NotUsed] = ???

  override def handleStreamedIn(commandName: String, stream: Source[MessageEnvelope[Any], NotUsed]): Action.Effect[_] =
    ???

  override def handleStreamed(
      commandName: String,
      stream: Source[MessageEnvelope[Any], NotUsed]): Source[Action.Effect[_], NotUsed] = ???
}
