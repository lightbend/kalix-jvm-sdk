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

package kalix.javasdk.impl.valueentity

import java.lang.reflect.Method

import kalix.javasdk.action.Action
import kalix.javasdk.valueentity.CommandContext
import kalix.javasdk.valueentity.ValueEntity

class ValueEntityReflectiveRouter[S, E <: ValueEntity[S]](override protected val entity: E)
    extends ValueEntityRouter[S, E](entity) {

  private val stateType = entity.getClass.getDeclaredMethod("emptyState").getReturnType
  private val allHandlers: Map[String, Method] =
    entity.getClass.getDeclaredMethods.toList
      // handlers are all methods returning Effect
      .filter(_.getReturnType == classOf[ValueEntity.Effect[_]])
      // value entities have only two input param
      .filter { method =>
        method.getParameters.length == 2 &&
        method.getParameterTypes()(0) == stateType // first param must be the state
      }
      .map { javaMethod => (javaMethod.getName.capitalize, javaMethod) }
      .toMap

  private def methodLookup(commandName: String) =
    allHandlers.getOrElse(commandName, throw new RuntimeException(s"no matching method for '$commandName'"))

  override protected def handleCommand(
      commandName: String,
      state: S,
      command: Any,
      context: CommandContext): ValueEntity.Effect[_] =
    methodLookup(commandName)
      .invoke(entity, state, command)
      .asInstanceOf[ValueEntity.Effect[_]]
}
