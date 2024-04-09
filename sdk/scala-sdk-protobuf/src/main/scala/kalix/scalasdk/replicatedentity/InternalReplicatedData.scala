/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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
