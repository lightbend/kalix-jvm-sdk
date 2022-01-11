
package com.example.replicated.multimap.multi_map_api

import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityContext
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityOptions
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityProvider
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedMultiMap
import com.example.replicated.multimap.domain.multi_map_domain.MultiMapDomainProto
import com.example.replicated.multimap.multi_map_api
import com.google.protobuf.Descriptors
import com.google.protobuf.empty.EmptyProto

import scala.collection.immutable.Seq

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * A replicated entity provider that defines how to register and create the entity for
 * the Protobuf service `MultiMapService`.
 *
 * Should be used with the `register` method in [[com.akkaserverless.scalasdk.AkkaServerless]].
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
    extends ReplicatedEntityProvider[ReplicatedMultiMap[com.example.replicated.multimap.domain.multi_map_domain.SomeKey, com.example.replicated.multimap.domain.multi_map_domain.SomeValue], MultiMapServiceEntity] {

  override def entityType: String = "some-multi-map"

  override def newRouter(context: ReplicatedEntityContext): MultiMapServiceEntityRouter =
    new MultiMapServiceEntityRouter(entityFactory(context))

  override def serviceDescriptor: Descriptors.ServiceDescriptor =
    MultiMapApiProto.javaDescriptor.findServiceByName("MultiMapService")

  override def additionalDescriptors: Seq[Descriptors.FileDescriptor] =
    EmptyProto.javaDescriptor :: MultiMapApiProto.javaDescriptor :: MultiMapDomainProto.javaDescriptor :: Nil
}
