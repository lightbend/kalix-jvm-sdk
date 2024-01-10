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

package kalix.javasdk.impl.replicatedentity

import kalix.javasdk.replicatedentity._
import kalix.javasdk.impl.AnySupport
import kalix.replicatedentity.ReplicatedData

final class ReplicatedDataFactoryImpl(anySupport: AnySupport) extends ReplicatedDataFactory {

  override def newCounter(): ReplicatedCounter =
    new ReplicatedCounterImpl

  override def newReplicatedCounterMap[K](): ReplicatedCounterMap[K] =
    new ReplicatedCounterMapImpl[K](anySupport)

  override def newReplicatedSet[T](): ReplicatedSet[T] =
    new ReplicatedSetImpl[T](anySupport)

  override def newRegister[T](value: T): ReplicatedRegister[T] =
    new ReplicatedRegisterImpl[T](anySupport, value, Option(value).map(anySupport.encodeScala))

  override def newReplicatedRegisterMap[K, V](): ReplicatedRegisterMap[K, V] =
    new ReplicatedRegisterMapImpl[K, V](anySupport)

  override def newReplicatedMultiMap[K, V](): ReplicatedMultiMap[K, V] =
    new ReplicatedMultiMapImpl[K, V](anySupport)

  override def newReplicatedMap[K, V <: ReplicatedData](): ReplicatedMap[K, V] =
    new ReplicatedMapImpl[K, InternalReplicatedData](anySupport).asInstanceOf[ReplicatedMap[K, V]]

  override def newVote(): ReplicatedVote =
    new ReplicatedVoteImpl
}
