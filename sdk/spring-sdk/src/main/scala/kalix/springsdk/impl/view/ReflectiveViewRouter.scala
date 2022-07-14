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
import kalix.javasdk.Metadata
import kalix.javasdk.impl.view.ViewRouter
import kalix.javasdk.view.View
import kalix.springsdk.impl.ComponentMethod
import kalix.springsdk.impl.InvocationContext

class ReflectiveViewRouter[S, V <: View[S]](view: V, componentMethods: Map[String, ComponentMethod])
    extends ViewRouter[S, V](view) {

  private def methodLookup(commandName: String) =
    componentMethods.getOrElse(commandName, throw new RuntimeException(s"no matching method for '$commandName'"))

  override def handleUpdate(commandName: String, state: S, event: Any): View.UpdateEffect[S] = {
    val componentMethod = methodLookup(commandName)
    val context =
      InvocationContext(
        event.asInstanceOf[ScalaPbAny],
        componentMethod.requestMessageDescriptor,
        Metadata.EMPTY
      ) // FIXME no metadata available???

    // safe call: if component method is None, proxy won't forward calls to it
    // typically, that happens when we have a View update method with transform = false
    // in such a case, the proxy can index the view payload directly, without passing through the user function
    componentMethod.method.get
      .invoke(view, componentMethod.parameterExtractors.map(e => e.extract(context)): _*)
      .asInstanceOf[View.UpdateEffect[S]]
  }

}
