/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.action

import com.google.protobuf.Descriptors
import kalix.scalasdk.impl.action.ActionRouter

trait ActionProvider[A <: Action] {
  def options: ActionOptions

  def serviceDescriptor: Descriptors.ServiceDescriptor

  def newRouter(context: ActionCreationContext): ActionRouter[A]

  def additionalDescriptors: Seq[Descriptors.FileDescriptor]
}
