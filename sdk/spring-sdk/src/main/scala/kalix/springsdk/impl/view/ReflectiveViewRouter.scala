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
import kalix.javasdk.impl.view.ViewRouter
import kalix.javasdk.view.View
import kalix.springsdk.impl.CommandHandler
import kalix.springsdk.impl.InvocationContext

class ReflectiveViewRouter[S, V <: View[S]](view: V, commandHandlers: Map[String, CommandHandler])
    extends ViewRouter[S, V](view) {

  private def commandHandlerLookup(commandName: String) =
    commandHandlers.getOrElse(commandName, throw new RuntimeException(s"no matching method for '$commandName'"))

  override def handleUpdate(commandName: String, state: S, event: Any): View.UpdateEffect[S] = {

    val viewStateType: Class[S] =
      this.view.getClass.getGenericSuperclass
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

    val commandHandler = commandHandlerLookup(commandName)
    val context =
      InvocationContext(event.asInstanceOf[ScalaPbAny], commandHandler.requestMessageDescriptor)

    commandHandler
      .lookupInvoker(event.asInstanceOf[ScalaPbAny].typeUrl)
      .invoke(view, context)
      .asInstanceOf[View.UpdateEffect[S]]
  }

}
