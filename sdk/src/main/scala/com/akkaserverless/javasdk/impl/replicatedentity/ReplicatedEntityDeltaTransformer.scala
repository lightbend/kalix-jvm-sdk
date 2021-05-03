/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl.replicatedentity

import com.akkaserverless.javasdk.impl.AnySupport
import com.akkaserverless.protocol.replicated_entity.ReplicatedEntityDelta

private[replicatedentity] object ReplicatedEntityDeltaTransformer {

  def create(delta: ReplicatedEntityDelta, anySupport: AnySupport): InternalReplicatedData = {
    val entity = delta.delta match {
      case ReplicatedEntityDelta.Delta.Gcounter(_) =>
        new GCounterImpl
      case ReplicatedEntityDelta.Delta.Pncounter(_) =>
        new PNCounterImpl
      case ReplicatedEntityDelta.Delta.Gset(_) =>
        new GSetImpl[Any](anySupport)
      case ReplicatedEntityDelta.Delta.Orset(_) =>
        new ORSetImpl[Any](anySupport)
      case ReplicatedEntityDelta.Delta.Flag(_) =>
        new FlagImpl
      case ReplicatedEntityDelta.Delta.Lwwregister(_) =>
        new LWWRegisterImpl[Any](anySupport)
      case ReplicatedEntityDelta.Delta.Ormap(_) =>
        new ORMapImpl[Any, InternalReplicatedData](anySupport)
      case ReplicatedEntityDelta.Delta.Vote(_) =>
        new VoteImpl
    }
    entity.applyDelta(delta.delta)
    entity
  }

}
