
package com.example.replicated.multimap

import com.example.replicated.multimap
import com.example.replicated.multimap.domain.MultiMapDomainProto
import com.google.protobuf.Descriptors
import com.google.protobuf.empty.EmptyProto
import kalix.scalasdk.replicatedentity.ReplicatedEntityContext
import kalix.scalasdk.replicatedentity.ReplicatedEntityOptions
import kalix.scalasdk.replicatedentity.ReplicatedEntityProvider
import kalix.scalasdk.replicatedentity.ReplicatedMultiMap

import scala.collection.immutable.Seq

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * A replicated entity provider that defines how to register and create the entity for
 * the Protobuf service `MultiMapService`.
 *
 * Should be used with the `register` method in [[kalix.scalasdk.Kalix]].
 */
object MultiMapServiceEntityProvider {
  def apply(entityFactory: ReplicatedEntityContext => MultiMapServiceEntity): MultiMapServiceEntityProvider =
    new MultiMapServiceEntityProvider(entityFactory, ReplicatedEntityOptions.defaults)

  def apply(entityFactory: ReplicatedEntityContext => MultiMapServiceEntity, options: ReplicatedEntityOptions): MultiMapServiceEntityProvider =
    new MultiMapServiceEntityProvider(entityFactory, options)
}


class MultiMapServiceEntityProvider private (
    entityFactory: ReplicatedEntityContext => MultiMapServiceEntity,
    override val options: ReplicatedEntityOptions)
    extends ReplicatedEntityProvider[ReplicatedMultiMap[com.example.replicated.multimap.domain.SomeKey, com.example.replicated.multimap.domain.SomeValue], MultiMapServiceEntity] {

  override final val typeId: String = "some-multi-map"

  override def newRouter(context: ReplicatedEntityContext): MultiMapServiceEntityRouter =
    new MultiMapServiceEntityRouter(entityFactory(context))

  override def serviceDescriptor: Descriptors.ServiceDescriptor =
    MultiMapApiProto.javaDescriptor.findServiceByName("MultiMapService")

  override def additionalDescriptors: Seq[Descriptors.FileDescriptor] =
    EmptyProto.javaDescriptor :: MultiMapApiProto.javaDescriptor :: MultiMapDomainProto.javaDescriptor :: Nil
}
