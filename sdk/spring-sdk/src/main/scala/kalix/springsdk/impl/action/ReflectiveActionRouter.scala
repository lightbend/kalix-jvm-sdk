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

import akka.NotUsed
import akka.stream.javadsl.Source
import com.google.protobuf.any.{ Any => ScalaPbAny }
import kalix.javasdk.action.Action
import kalix.javasdk.action.MessageEnvelope
import kalix.javasdk.impl.action.ActionRouter
import kalix.springsdk.impl.ComponentMethod
import kalix.springsdk.impl.InvocationContext
import reactor.core.publisher.Flux

class ReflectiveActionRouter[A <: Action](action: A, componentMethods: Map[String, ComponentMethod])
    extends ActionRouter[A](action) {

  private def methodLookup(commandName: String) =
    componentMethods.getOrElse(commandName, throw new RuntimeException(s"no matching method for '$commandName'"))

  override def handleUnary(commandName: String, message: MessageEnvelope[Any]): Action.Effect[_] = {

    val componentMethod = methodLookup(commandName)
    val context =
      InvocationContext(
        message.payload().asInstanceOf[ScalaPbAny],
        componentMethod.requestMessageDescriptor,
        message.metadata())

    // safe call: if component method is None, proxy won't forward calls to it
    // typically, that happens when we have a View update method with transform = false
    // in such a case, the proxy can index the view payload directly, without passing through the user function
    componentMethod.method.get
      .invoke(action, componentMethod.parameterExtractors.map(e => e.extract(context)): _*)
      .asInstanceOf[Action.Effect[_]]
  }

  // TODO: to implement
  override def handleStreamedOut(
      commandName: String,
      message: MessageEnvelope[Any]): Source[Action.Effect[_], NotUsed] = {
    val componentMethod = methodLookup(commandName)
    val context =
      InvocationContext(
        message.payload().asInstanceOf[ScalaPbAny],
        componentMethod.requestMessageDescriptor,
        message.metadata())

    // safe call: if component method is None, proxy won't forward calls to it
    // typically, that happens when we have a View update method with transform = false
    // in such a case, the proxy can index the view payload directly, without passing through the user function
    val response =
      componentMethod.method.get
        .invoke(action, componentMethod.parameterExtractors.map(e => e.extract(context)): _*)
        .asInstanceOf[Flux[Action.Effect[_]]]

    Source.fromPublisher(response)
  }

  // TODO: to implement
  override def handleStreamedIn(commandName: String, stream: Source[MessageEnvelope[Any], NotUsed]): Action.Effect[_] =
    ???

  // TODO: to implement
  override def handleStreamed(
      commandName: String,
      stream: Source[MessageEnvelope[Any], NotUsed]): Source[Action.Effect[_], NotUsed] = ???
}
