/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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
