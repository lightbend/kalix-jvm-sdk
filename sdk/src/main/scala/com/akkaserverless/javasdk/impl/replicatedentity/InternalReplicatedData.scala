/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl.replicatedentity

import com.akkaserverless.javasdk.replicatedentity.ReplicatedData
import com.akkaserverless.protocol.replicated_entity.ReplicatedEntityDelta

private[replicatedentity] trait InternalReplicatedData extends ReplicatedData {
  def name: String
  def hasDelta: Boolean
  def delta: ReplicatedEntityDelta.Delta
  def resetDelta(): Unit
  def applyDelta: PartialFunction[ReplicatedEntityDelta.Delta, Unit]
}
