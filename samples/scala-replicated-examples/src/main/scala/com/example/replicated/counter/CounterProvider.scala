package com.example.replicated.counter

import scala.collection.immutable

import com.akkaserverless.scalasdk.impl.replicatedentity.ReplicatedEntityHandler
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedCounter
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityContext
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityOptions
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityProvider
import com.google.protobuf.Descriptors
import com.google.protobuf.EmptyProto

object CounterProvider {
  def apply(actionFactory: ReplicatedEntityContext => Counter): CounterProvider =
    new CounterProvider(actionFactory, ReplicatedEntityOptions.defaults)

  def apply(actionFactory: ReplicatedEntityContext => Counter, options: ReplicatedEntityOptions): CounterProvider =
    new CounterProvider(actionFactory, options)

}

class CounterProvider private (
    factory: ReplicatedEntityContext => Counter,
    override val options: ReplicatedEntityOptions)
    extends ReplicatedEntityProvider[ReplicatedCounter, Counter] {

  override def entityType: String = "counter"

  override def newHandler(context: ReplicatedEntityContext): ReplicatedEntityHandler[ReplicatedCounter, Counter] =
    new CounterHandler(factory(context))

  override def serviceDescriptor: Descriptors.ServiceDescriptor =
    CounterApiProto.javaDescriptor.findServiceByName("CounterService")

  override def additionalDescriptors: immutable.Seq[Descriptors.FileDescriptor] =
    immutable.Seq(EmptyProto.getDescriptor, CounterApiProto.javaDescriptor)
}
