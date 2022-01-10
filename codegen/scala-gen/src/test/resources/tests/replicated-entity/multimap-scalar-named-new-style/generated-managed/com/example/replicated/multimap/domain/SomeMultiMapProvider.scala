
package com.example.replicated.multimap.domain

import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityContext
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityOptions
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityProvider
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedMultiMap
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
object SomeMultiMapProvider {
  def apply(entityFactory: ReplicatedEntityContext => SomeMultiMap): SomeMultiMapProvider =
    new SomeMultiMapProvider(entityFactory, ReplicatedEntityOptions.defaults)

  def apply(entityFactory: ReplicatedEntityContext => SomeMultiMap, options: ReplicatedEntityOptions): SomeMultiMapProvider =
    new SomeMultiMapProvider(entityFactory, options)
}


class SomeMultiMapProvider private (
    entityFactory: ReplicatedEntityContext => SomeMultiMap,
    override val options: ReplicatedEntityOptions)
    extends ReplicatedEntityProvider[ReplicatedMultiMap[String, Double], SomeMultiMap] {

  override def entityType: String = "some-multi-map"

  override def newRouter(context: ReplicatedEntityContext): SomeMultiMapRouter =
    new SomeMultiMapRouter(entityFactory(context))

  override def serviceDescriptor: Descriptors.ServiceDescriptor =
    multi_map_api.MultiMapApiProto.javaDescriptor.findServiceByName("MultiMapService")

  override def additionalDescriptors: Seq[Descriptors.FileDescriptor] =
    EmptyProto.javaDescriptor :: multi_map_api.MultiMapApiProto.javaDescriptor :: Nil
}
