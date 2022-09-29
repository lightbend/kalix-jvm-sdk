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

import com.google.protobuf.any.{ Any => ScalaPbAny }
import kalix.javasdk.{ JsonSupport, Metadata }
import kalix.javasdk.impl.view.ViewRouter
import kalix.javasdk.view.View
import kalix.springsdk.impl.ComponentMethod
import kalix.springsdk.impl.InvocationContext

import java.lang.reflect.ParameterizedType
import scala.PartialFunction.condOpt

class ReflectiveViewRouter[S, V <: View[S]](view: V, componentMethods: Map[String, ComponentMethod])
    extends ViewRouter[S, V](view) {

  private def methodLookup(commandName: String) =
    componentMethods.getOrElse(commandName, throw new RuntimeException(s"no matching method for '$commandName'"))

  override def handleUpdate(commandName: String, state: S, event: Any): View.UpdateEffect[S] = {
    val componentMethod = methodLookup(commandName)
    val viewStateType = this.view.getClass.getGenericSuperclass
      .asInstanceOf[ParameterizedType]
      .getActualTypeArguments
      .head
      .asInstanceOf[Class[_]]

    val context =
      InvocationContext(
        event.asInstanceOf[ScalaPbAny],
        componentMethod.requestMessageDescriptor,
        Metadata.EMPTY
      ) // FIXME no metadata available???

    // the state: S received can either be of the view "state" type (if coming from emptyState)
    // or PB Any type (if coming from the proxy)
    val newState = condOpt(state) {
      case s if s != null && state.getClass == viewStateType => s
      case s if s != null => JsonSupport.decodeJson(viewStateType, ScalaPbAny.toJavaProto(s.asInstanceOf[ScalaPbAny]))
    }

    val javaMethod = componentMethod
      .lookupMethod(event.asInstanceOf[ScalaPbAny].typeUrl)

    val params = javaMethod.parameterExtractors.map(e => e.extract(context))

    // safe call: if component method is None, proxy won't forward calls to it
    // typically, that happens when we have a View update method with transform = false
    // in such a case, the proxy can index the view payload directly, without passing through the user function
    (javaMethod.method.get.getParameterCount, newState) match {
      case (1, _) =>
        javaMethod.method.get
          .invoke(view, params: _*)
          .asInstanceOf[View.UpdateEffect[S]]
      case (2, Some(s)) =>
        javaMethod.method.get
          .invoke(view, s, params.head)
          .asInstanceOf[View.UpdateEffect[S]]
      case (2, None) =>
        javaMethod.method.get
          .invoke(view, null, params.head)
          .asInstanceOf[View.UpdateEffect[S]]
      case (n, _) => // this shouldn't really be reached
        throw new RuntimeException(s"unexpected number of params ($n) for '$commandName'")
    }
  }

}
