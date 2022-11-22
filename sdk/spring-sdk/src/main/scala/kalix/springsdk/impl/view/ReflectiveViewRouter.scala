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

package kalix.springsdk.impl.view

import java.lang.reflect.ParameterizedType

import com.google.protobuf.any.{ Any => ScalaPbAny }
import kalix.javasdk.JsonSupport
import kalix.javasdk.impl.AnySupport.ProtobufEmptyTypeUrl
import kalix.javasdk.impl.view.ViewRouter
import kalix.javasdk.impl.view.ViewUpdateEffectImpl
import kalix.javasdk.view.View
import kalix.springsdk.impl.CommandHandler
import kalix.springsdk.impl.ComponentDescriptorFactory.findTableName
import kalix.springsdk.impl.InvocationContext

class ReflectiveViewRouter[V <: View](view: V, commandHandlers: Map[String, CommandHandler], ignoreUnknown: Boolean)
    extends ViewRouter(view) {

  private def commandHandlerLookup(commandName: String) =
    commandHandlers.getOrElse(commandName, throw new RuntimeException(s"no matching method for '$commandName'"))

  override def handleUpdate[S](commandName: String, state: S, event: Any): View.UpdateEffect[S] = {

    val commandHandler = commandHandlerLookup(commandName)

    val anyEvent = event.asInstanceOf[ScalaPbAny]
    val inputTypeUrl = anyEvent.typeUrl
    val methodInvoker = commandHandler.lookupInvoker(inputTypeUrl)

    methodInvoker match {
      case Some(invoker) =>
        val viewStateType = invoker.method.getGenericReturnType
          .asInstanceOf[ParameterizedType]
          .getActualTypeArguments
          .head
          .asInstanceOf[Class[S]]

        // the state: S received can either be of the view "state" type (if coming from emptyState)
        // or PB Any type (if coming from the proxy)
        state match {
          case s if s == null || state.getClass == viewStateType =>
            // note that we set the state even if null, this is needed in order to
            // be able to call viewState() later
            view._internalSetViewState(s)
          case s =>
            val deserializedState =
              JsonSupport.decodeJson(viewStateType, ScalaPbAny.toJavaProto(s.asInstanceOf[ScalaPbAny]))
            view._internalSetViewState(deserializedState)
        }

        inputTypeUrl match {
          case ProtobufEmptyTypeUrl =>
            invoker
              .invoke(view)
              .asInstanceOf[View.UpdateEffect[S]]
          case _ =>
            val context =
              InvocationContext(anyEvent, commandHandler.requestMessageDescriptor)
            invoker
              .invoke(view, context)
              .asInstanceOf[View.UpdateEffect[S]]
        }
      case None if ignoreUnknown => ViewUpdateEffectImpl.builder().ignore()
      case None =>
        throw new NoSuchElementException(s"Couldn't find any method with input type [$inputTypeUrl] in View [$view].")
    }
  }

  override def viewTable(commandName: String, event: Any): String = {
    commandHandlerLookup(commandName).lookupInvoker(event.asInstanceOf[ScalaPbAny].typeUrl) match {
      case Some(invoker) => findTableName(view.getClass, invoker.method)
      case None          => ""
    }
  }
}
