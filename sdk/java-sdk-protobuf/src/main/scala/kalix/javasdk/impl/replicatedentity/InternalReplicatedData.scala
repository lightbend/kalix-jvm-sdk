/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.replicatedentity

import kalix.protocol.replicated_entity.ReplicatedEntityDelta
import kalix.replicatedentity.ReplicatedData

private[kalix] trait InternalReplicatedData extends ReplicatedData {
  type Self <: InternalReplicatedData
  def name: String
  def hasDelta: Boolean
  def getDelta: ReplicatedEntityDelta.Delta
  def resetDelta(): Self
  def applyDelta: PartialFunction[ReplicatedEntityDelta.Delta, Self]
}
