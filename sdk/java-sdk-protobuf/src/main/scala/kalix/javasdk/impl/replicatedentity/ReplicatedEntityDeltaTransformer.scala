/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.replicatedentity

import kalix.javasdk.impl.AnySupport
import kalix.protocol.replicated_entity.ReplicatedEntityDelta

private[replicatedentity] object ReplicatedEntityDeltaTransformer {

  def create(delta: ReplicatedEntityDelta, anySupport: AnySupport): InternalReplicatedData = {
    val data = delta.delta match {
      case ReplicatedEntityDelta.Delta.Counter(_) =>
        new ReplicatedCounterImpl
      case ReplicatedEntityDelta.Delta.ReplicatedSet(_) =>
        new ReplicatedSetImpl[Any](anySupport)
      case ReplicatedEntityDelta.Delta.Register(_) =>
        new ReplicatedRegisterImpl[Any](anySupport)
      case ReplicatedEntityDelta.Delta.ReplicatedMap(_) =>
        new ReplicatedMapImpl[Any, InternalReplicatedData](anySupport)
      case ReplicatedEntityDelta.Delta.ReplicatedCounterMap(_) =>
        new ReplicatedCounterMapImpl[Any](anySupport)
      case ReplicatedEntityDelta.Delta.ReplicatedRegisterMap(_) =>
        new ReplicatedRegisterMapImpl[Any, Any](anySupport)
      case ReplicatedEntityDelta.Delta.ReplicatedMultiMap(_) =>
        new ReplicatedMultiMapImpl[Any, Any](anySupport)
      case ReplicatedEntityDelta.Delta.Vote(_) =>
        new ReplicatedVoteImpl
      case _ =>
        throw new RuntimeException(s"Received unexpected replicated entity delta: ${delta.delta}")
    }
    data.applyDelta(delta.delta)
  }

}
