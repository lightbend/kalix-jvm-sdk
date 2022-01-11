package org.example.valueentity.counter_api

import com.akkaserverless.scalasdk.valueentity.ValueEntityContext
import com.akkaserverless.scalasdk.valueentity.ValueEntityOptions
import com.akkaserverless.scalasdk.valueentity.ValueEntityProvider
import com.google.protobuf.Descriptors
import com.google.protobuf.empty.EmptyProto
import org.example.valueentity.counter_api
import org.example.valueentity.domain.counter_domain.CounterDomainProto
import org.example.valueentity.domain.counter_domain.CounterState

import scala.collection.immutable.Seq

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object CounterServiceEntityProvider {
  def apply(entityFactory: ValueEntityContext => CounterServiceEntity): CounterServiceEntityProvider =
    new CounterServiceEntityProvider(entityFactory, ValueEntityOptions.defaults)
}
class CounterServiceEntityProvider private(entityFactory: ValueEntityContext => CounterServiceEntity, override val options: ValueEntityOptions)
  extends ValueEntityProvider[CounterState, CounterServiceEntity] {

  def withOptions(newOptions: ValueEntityOptions): CounterServiceEntityProvider =
    new CounterServiceEntityProvider(entityFactory, newOptions)

  override final val serviceDescriptor: Descriptors.ServiceDescriptor =
    CounterApiProto.javaDescriptor.findServiceByName("CounterService")

  override final val entityType = "counter"

  override final def newRouter(context: ValueEntityContext): CounterServiceEntityRouter =
    new CounterServiceEntityRouter(entityFactory(context))

  override final val additionalDescriptors: Seq[Descriptors.FileDescriptor] =
    CounterDomainProto.javaDescriptor ::
    CounterApiProto.javaDescriptor ::
    EmptyProto.javaDescriptor :: Nil
}

