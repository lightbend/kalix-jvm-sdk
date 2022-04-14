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

package com.akkaserverless.javasdk.testkit.impl

import akka.stream.Materializer
import com.akkaserverless.javasdk.Metadata
import com.akkaserverless.javasdk.valueentity.CommandContext
import com.akkaserverless.javasdk.valueentity.ValueEntityContext
import com.akkaserverless.javasdk.impl.InternalContext

/**
 * INTERNAL API Used by the generated testkit
 */
final class TestKitValueEntityCommandContext(
    override val entityId: String,
    override val commandName: String = "stubCommandName",
    override val commandId: Long = 0L,
    override val metadata: Metadata = Metadata.EMPTY)
    extends AbstractTestKitContext
    with ValueEntityContext
    with CommandContext {

  def this(entityId: String, metadata: Metadata) {
    this(entityId = entityId, metadata = metadata, commandName = "stubCommandName")
  }

}
