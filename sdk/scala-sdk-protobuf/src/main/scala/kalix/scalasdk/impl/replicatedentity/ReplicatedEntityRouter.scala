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

package kalix.scalasdk.impl.replicatedentity

import kalix.scalasdk.replicatedentity.ReplicatedEntity
import kalix.replicatedentity.ReplicatedData
import kalix.scalasdk.replicatedentity.CommandContext

/**
 * INTERNAL API, but used by generated code.
 */
abstract class ReplicatedEntityRouter[D <: ReplicatedData, E <: ReplicatedEntity[D]](val entity: E) {

  def handleCommand(commandName: String, data: D, command: Any, context: CommandContext): ReplicatedEntity.Effect[_]
}
