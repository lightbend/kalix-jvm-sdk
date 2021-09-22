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

package com.akkaserverless.scalasdk.impl.valueentity

import com.akkaserverless.scalasdk.valueentity.CommandContext
import com.akkaserverless.scalasdk.valueentity.ValueEntity

object ValueEntityHandler {
  final case class CommandHandlerNotFound(commandName: String) extends RuntimeException
}

abstract class ValueEntityHandler[S, E <: ValueEntity[S]](val entity: E) {
  def handleCommand(commandName: String, state: S, command: Any, context: CommandContext): ValueEntity.Effect[_]
}
