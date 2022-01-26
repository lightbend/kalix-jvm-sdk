package org.example.eventsourcedentity

import com.akkaserverless.javasdk.impl.Serializer
import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityContext
import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityOptions
import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityProvider
import com.google.protobuf.Descriptors
import com.google.protobuf.empty.EmptyProto
import org.example.eventsourcedentity
import org.example.eventsourcedentity.domain.CounterDomainProto
import org.example.eventsourcedentity.domain.CounterState

import scala.collection.immutable.Seq

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object CounterServiceEntityProvider {
  def apply(entityFactory: EventSourcedEntityContext => CounterServiceEntity): CounterServiceEntityProvider =
    new CounterServiceEntityProvider(entityFactory, EventSourcedEntityOptions.defaults)
}
class CounterServiceEntityProvider private(entityFactory: EventSourcedEntityContext => CounterServiceEntity, override val options: EventSourcedEntityOptions)
  extends EventSourcedEntityProvider[CounterState, CounterServiceEntity] {

  def withOptions(newOptions: EventSourcedEntityOptions): CounterServiceEntityProvider =
    new CounterServiceEntityProvider(entityFactory, newOptions)

  override final val serviceDescriptor: Descriptors.ServiceDescriptor =
    CounterApiProto.javaDescriptor.findServiceByName("CounterService")

  override final val entityType: String = "counter"

  override final def newRouter(context: EventSourcedEntityContext): CounterServiceEntityRouter =
    new CounterServiceEntityRouter(entityFactory(context))

  override final val additionalDescriptors: Seq[Descriptors.FileDescriptor] =
    CounterApiProto.javaDescriptor ::
    EmptyProto.javaDescriptor ::
    CounterDomainProto.javaDescriptor :: Nil

  override def serializer: Serializer =
    Serializer.noopSerializer
}

