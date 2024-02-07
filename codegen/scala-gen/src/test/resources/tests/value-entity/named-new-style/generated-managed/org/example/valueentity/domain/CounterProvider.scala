package org.example.valueentity.domain

import com.google.protobuf.Descriptors
import com.google.protobuf.empty.EmptyProto
import kalix.scalasdk.valueentity.ValueEntityContext
import kalix.scalasdk.valueentity.ValueEntityOptions
import kalix.scalasdk.valueentity.ValueEntityProvider
import org.example.valueentity

import scala.collection.immutable.Seq

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object CounterProvider {
  def apply(entityFactory: ValueEntityContext => Counter): CounterProvider =
    new CounterProvider(entityFactory, ValueEntityOptions.defaults)
}
class CounterProvider private(entityFactory: ValueEntityContext => Counter, override val options: ValueEntityOptions)
  extends ValueEntityProvider[CounterState, Counter] {

  def withOptions(newOptions: ValueEntityOptions): CounterProvider =
    new CounterProvider(entityFactory, newOptions)

  override final val serviceDescriptor: Descriptors.ServiceDescriptor =
    valueentity.CounterApiProto.javaDescriptor.findServiceByName("CounterService")

  override final val typeId: String = "counter"

  override final def newRouter(context: ValueEntityContext): CounterRouter =
    new CounterRouter(entityFactory(context))

  override final val additionalDescriptors: Seq[Descriptors.FileDescriptor] =
    valueentity.CounterApiProto.javaDescriptor ::
    EmptyProto.javaDescriptor ::
    CounterDomainProto.javaDescriptor :: Nil
}

