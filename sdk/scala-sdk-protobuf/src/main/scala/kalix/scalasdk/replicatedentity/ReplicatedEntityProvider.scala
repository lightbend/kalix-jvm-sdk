/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.replicatedentity

import kalix.replicatedentity.ReplicatedData
import kalix.scalasdk.impl.replicatedentity.ReplicatedEntityRouter
import com.google.protobuf.Descriptors

trait ReplicatedEntityProvider[D <: ReplicatedData, E <: ReplicatedEntity[D]] {

  def typeId: String
  def options: ReplicatedEntityOptions
  def newRouter(context: ReplicatedEntityContext): ReplicatedEntityRouter[D, E]

  def serviceDescriptor: Descriptors.ServiceDescriptor
  def additionalDescriptors: Seq[Descriptors.FileDescriptor]
}
