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

package com.akkaserverless.javasdk.impl.replicatedentity

import com.akkaserverless.javasdk.replicatedentity._
import com.akkaserverless.javasdk.impl.AnySupport

trait AbstractReplicatedEntityFactory extends ReplicatedDataFactory {
  protected def anySupport: AnySupport
  protected def newEntity[E <: InternalReplicatedData](entity: E): E
  override def newGCounter(): GCounter = newEntity(new GCounterImpl)
  override def newPNCounter(): PNCounter = newEntity(new PNCounterImpl)
  override def newGSet[T](): GSet[T] = newEntity(new GSetImpl[T](anySupport))
  override def newORSet[T](): ORSet[T] = newEntity(new ORSetImpl[T](anySupport))
  override def newFlag(): Flag = newEntity(new FlagImpl)
  override def newLWWRegister[T](value: T): LWWRegister[T] = {
    val register = newEntity(new LWWRegisterImpl[T](anySupport))
    if (value != null) {
      register.set(value)
    }
    register
  }
  override def newORMap[K, V <: ReplicatedData](): ORMap[K, V] =
    newEntity(new ORMapImpl[K, InternalReplicatedData](anySupport)).asInstanceOf[ORMap[K, V]]
  override def newVote(): Vote = newEntity(new VoteImpl)
}
