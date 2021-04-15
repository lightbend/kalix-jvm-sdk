/*
 * Copyright 2019 Lightbend Inc.
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
