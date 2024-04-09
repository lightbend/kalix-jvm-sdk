/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.view

import com.google.protobuf.Descriptors
import scala.collection.immutable.Seq

import kalix.scalasdk.impl.view.ViewUpdateRouter

trait ViewProvider {
  def serviceDescriptor: Descriptors.ServiceDescriptor

  def viewId: String

  def options: ViewOptions

  def newRouter(context: ViewCreationContext): ViewUpdateRouter

  def additionalDescriptors: Seq[Descriptors.FileDescriptor]
}
