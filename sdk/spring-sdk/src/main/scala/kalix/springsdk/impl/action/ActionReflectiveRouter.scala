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
import com.google.protobuf.{ Descriptors, DynamicMessage }
import kalix.javasdk.Metadata
import kalix.javasdk.action.Action
import kalix.javasdk.action.MessageEnvelope
import kalix.javasdk.impl.action.ActionRouter
import kalix.springsdk.impl.reflection.{ DynamicMessageContext, MetadataContext, ParameterExtractor }
import com.google.protobuf.any.{ Any => ScalaPbAny }

class ActionReflectiveRouter[A <: Action](action: A, methods: Map[String, ActionMethod])
    extends ActionRouter[A](action) {

  private def methodLookup(commandName: String) =
    methods.getOrElse(commandName, throw new RuntimeException(s"no matching method for '$commandName'"))

  override def handleUnary(commandName: String, message: MessageEnvelope[Any]): Action.Effect[_] = {
    val method = methodLookup(commandName)
    // Todo - this should probably be elsewhere
    val anyMessage = message.payload().asInstanceOf[ScalaPbAny]
    val dynamicMessage = DynamicMessage.parseFrom(method.messageDescriptor, anyMessage.value)
    val context = new ActionInvocationContext(dynamicMessage, message.metadata())
    method.method
      .invoke(action, method.parameterExtractors.map(e => e.extract(context)): _*)
      .asInstanceOf[Action.Effect[_]]
  }

  override def handleStreamedOut(
      commandName: String,
      message: MessageEnvelope[Any]): Source[Action.Effect[_], NotUsed] = ???

  override def handleStreamedIn(commandName: String, stream: Source[MessageEnvelope[Any], NotUsed]): Action.Effect[_] =
    ???

  override def handleStreamed(
      commandName: String,
      stream: Source[MessageEnvelope[Any], NotUsed]): Source[Action.Effect[_], NotUsed] = ???
}

// Might need to have one of each of these for unary, streamed out, streamed in and streamed.
case class ActionMethod(
    method: Method,
    grpcMethodName: String,
    parameterExtractors: Array[ParameterExtractor[ActionInvocationContext, AnyRef]],
    messageDescriptor: Descriptors.Descriptor)

class ActionInvocationContext(val message: DynamicMessage, val metadata: Metadata)
    extends DynamicMessageContext
    with MetadataContext
