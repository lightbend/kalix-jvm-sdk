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

package kalix.javasdk.testkit.impl;

import kalix.javasdk.action.ActionContext

import java.util.{ HashMap, Optional }
import java.nio.ByteBuffer
import kalix.javasdk.action.ActionCreationContext
import kalix.javasdk.Metadata
import scala.collection.convert.ImplicitConversions._
import java.util.Optional
import java.util.concurrent.CompletionStage

import akka.Done
import kalix.javasdk.DeferredCall
import kalix.javasdk.action.ActionContext
import kalix.javasdk.action.ActionCreationContext
import kalix.javasdk.impl.InternalContext

/**
 * INTERNAL API Used by the generated testkit
 */
final class TestKitActionContext(metadata: Metadata, mockRegistry: TestKitMockRegistry = TestKitMockRegistry.empty)
    extends AbstractTestKitContext(mockRegistry)
    with ActionContext
    with ActionCreationContext
    with InternalContext {

  def this() {
    this(Metadata.EMPTY, TestKitMockRegistry.empty)
  }

  def this(metadata: Metadata) {
    this(metadata, TestKitMockRegistry.empty)
  }

  override def metadata() = metadata

  override def eventSubject() = metadata.get("ce-subject")

  override def getGrpcClient[T](clientClass: Class[T], service: String): T = getComponentGrpcClient(clientClass)

}
