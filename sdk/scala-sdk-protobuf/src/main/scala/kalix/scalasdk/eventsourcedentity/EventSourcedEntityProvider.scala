/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.eventsourcedentity

import kalix.scalasdk.impl.eventsourcedentity.EventSourcedEntityRouter
import com.google.protobuf.Descriptors

/**
 * Register an event sourced entity in {@link kalix.scalasdk.Kalix} using a <code>EventSourcedEntityProvider</code>. The
 * concrete <code>EventSourcedEntityProvider</code> is generated for the specific entities defined in Protobuf, for
 * example <code>CustomerEntityProvider </code>.
 */
trait EventSourcedEntityProvider[S, E <: EventSourcedEntity[S]] {
  def options: EventSourcedEntityOptions

  def serviceDescriptor: Descriptors.ServiceDescriptor

  def typeId: String

  def newRouter(context: EventSourcedEntityContext): EventSourcedEntityRouter[S, E]

  def additionalDescriptors: Seq[Descriptors.FileDescriptor]
}
