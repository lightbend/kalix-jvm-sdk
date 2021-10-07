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

package com.akkaserverless.scalasdk.impl.replicatedentity

import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntity
import com.akkaserverless.replicatedentity.ReplicatedData
import com.akkaserverless.scalasdk.replicatedentity.CommandContext

/**
 * INTERNAL API, but used by generated code.
 */
abstract class ReplicatedEntityHandler[D <: ReplicatedData, E <: ReplicatedEntity[D]](val entity: E) {

  def handleCommand(
      commandName: String,
      data: ReplicatedData,
      command: Any,
      context: CommandContext): ReplicatedEntity.Effect[_]
}
