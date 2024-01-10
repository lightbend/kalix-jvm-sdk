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

package kalix.scalasdk.replicatedentity

import kalix.javasdk.impl.replicatedentity.{ InternalReplicatedData => JavaSdkInternalReplicatedData }
import kalix.protocol.replicated_entity.ReplicatedEntityDelta

trait InternalReplicatedData extends JavaSdkInternalReplicatedData {

  def delegate: JavaSdkInternalReplicatedData

  override def name: String = delegate.name

  override def hasDelta: Boolean = delegate.hasDelta

  override def getDelta: ReplicatedEntityDelta.Delta = delegate.getDelta

}
