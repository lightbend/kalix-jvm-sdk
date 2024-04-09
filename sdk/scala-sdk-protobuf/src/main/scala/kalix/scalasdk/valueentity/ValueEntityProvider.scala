/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.valueentity

import kalix.scalasdk.impl.valueentity.ValueEntityRouter
import com.google.protobuf.Descriptors

/**
 * Register a value based entity in {@link kalix.scalasdk.Kalix} using a <code> ValueEntityProvider</code>. The concrete
 * <code>ValueEntityProvider</code> is generated for the specific entities defined in Protobuf, for example
 * <code>CustomerEntityProvider</code>.
 */
trait ValueEntityProvider[S, E <: ValueEntity[S]] {
  def options: ValueEntityOptions

  def serviceDescriptor: Descriptors.ServiceDescriptor

  def typeId: String

  def newRouter(context: ValueEntityContext): ValueEntityRouter[S, E]

  def additionalDescriptors: Seq[Descriptors.FileDescriptor]
}
