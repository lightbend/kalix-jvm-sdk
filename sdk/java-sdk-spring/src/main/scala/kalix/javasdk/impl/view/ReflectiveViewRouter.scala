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

package kalix.javasdk.impl.view

import java.lang.reflect.ParameterizedType
import java.util.{ Map => JMap }

import scala.jdk.CollectionConverters._

import com.google.protobuf.any.{ Any => ScalaPbAny }
import kalix.javasdk.JsonSupport
import kalix.javasdk.impl.AnySupport.ProtobufEmptyTypeUrl
import kalix.javasdk.impl.CommandHandler
import kalix.javasdk.impl.ComponentDescriptorFactory
import kalix.javasdk.impl.InvocationContext
import kalix.javasdk.view.View

class ReflectiveViewRouter[S, V <: View[S]](
    view: V,
    commandHandlers: Map[String, CommandHandler],
    ignoreUnknown: Boolean)
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

    val anyEvent = event.asInstanceOf[ScalaPbAny]
    val inputTypeUrl = anyEvent.typeUrl
    val methodInvoker = commandHandler.lookupInvoker(inputTypeUrl)

    methodInvoker match {
      case Some(invoker) =>
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

}

class ReflectiveViewMultiTableRouter(
    viewTables: JMap[Class[View[_]], View[_]],
    commandHandlers: Map[String, CommandHandler])
    extends ViewMultiTableRouter {

  private val routers: Map[Class[_], ReflectiveViewRouter[Any, View[Any]]] = viewTables.asScala.toMap.map {
    case (viewTableClass, viewTable) => viewTableClass -> createViewRouter(viewTableClass, viewTable)
  }

  private val commandRouters: Map[String, ReflectiveViewRouter[Any, View[Any]]] = commandHandlers.flatMap {
    case (commandName, commandHandler) =>
      commandHandler.methodInvokers.values.headOption.flatMap { methodInvoker =>
        routers.get(methodInvoker.method.getDeclaringClass).map(commandName -> _)
      }
  }

  private def createViewRouter(
      viewTableClass: Class[View[_]],
      viewTable: View[_]): ReflectiveViewRouter[Any, View[Any]] = {
    val ignoreUnknown = ComponentDescriptorFactory.findIgnore(viewTableClass)
    val tableCommandHandlers = commandHandlers.filter { case (_, commandHandler) =>
      commandHandler.methodInvokers.exists { case (_, methodInvoker) =>
        methodInvoker.method.getDeclaringClass eq viewTableClass
      }
    }
    new ReflectiveViewRouter(viewTable.asInstanceOf[View[Any]], tableCommandHandlers, ignoreUnknown)
  }

  override def viewRouter(commandName: String): ViewRouter[_, _] = {
    commandRouters.getOrElse(commandName, throw new RuntimeException(s"No view router for '$commandName'"))
  }
}
