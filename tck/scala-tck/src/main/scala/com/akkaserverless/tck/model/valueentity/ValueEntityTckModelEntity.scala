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

package com.akkaserverless.tck.model.valueentity

import com.akkaserverless.scalasdk.valueentity.ValueEntity
import com.akkaserverless.scalasdk.valueentity.ValueEntityContext
import com.akkaserverless.tck.model.valueentity

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

/** A value entity. */
class ValueEntityTckModelEntity(context: ValueEntityContext) extends AbstractValueEntityTckModelEntity {
  override def emptyState: Persisted =
    throw new UnsupportedOperationException("Not implemented yet, replace with your empty entity state")

  override def process(currentState: Persisted, request: Request): ValueEntity.Effect[Response] =
    effects.error("The command handler for `Process` is not implemented, yet")

}
