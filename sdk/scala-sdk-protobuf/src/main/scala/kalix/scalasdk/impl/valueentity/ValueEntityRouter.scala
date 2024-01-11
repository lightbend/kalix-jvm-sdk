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

package kalix.scalasdk.impl.valueentity

import kalix.scalasdk.valueentity.CommandContext
import kalix.scalasdk.valueentity.ValueEntity

/**
 * INTERNAL API, but used by generated code.
 */
abstract class ValueEntityRouter[S, E <: ValueEntity[S]](val entity: E) {
  def handleCommand(commandName: String, state: S, command: Any, context: CommandContext): ValueEntity.Effect[_]
}
