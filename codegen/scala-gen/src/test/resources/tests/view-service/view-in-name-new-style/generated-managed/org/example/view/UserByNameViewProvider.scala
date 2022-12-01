package org.example.view

import com.google.protobuf.Descriptors
import com.google.protobuf.EmptyProto
import kalix.javasdk.impl.view.UpdateHandlerNotFound
import kalix.scalasdk.impl.view.ViewRouter
import kalix.scalasdk.view.View
import kalix.scalasdk.view.ViewCreationContext
import kalix.scalasdk.view.ViewOptions
import kalix.scalasdk.view.ViewProvider

import scala.collection.immutable.Seq

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object UserByNameViewProvider {
  def apply(viewFactory: ViewCreationContext => UserByNameViewImpl): UserByNameViewProvider =
    new UserByNameViewProvider(viewFactory, viewId = "UserByNameView", options = ViewOptions.defaults)
}

class UserByNameViewProvider private(
    viewFactory: ViewCreationContext => UserByNameViewImpl,
    override val viewId: String,
    override val options: ViewOptions)
  extends ViewProvider {

  /**
   * Use a custom view identifier. By default, the viewId is the same as the proto service name.
   * A different identifier can be needed when making rolling updates with changes to the view definition.
   */
  def withViewId(viewId: String): UserByNameViewProvider =
    new UserByNameViewProvider(viewFactory, viewId, options)

  def withOptions(newOptions: ViewOptions): UserByNameViewProvider =
    new UserByNameViewProvider(viewFactory, viewId, newOptions)

  override final def serviceDescriptor: Descriptors.ServiceDescriptor =
    ExampleViewsProto.javaDescriptor.findServiceByName("UserByNameView")

  override final def newRouter(context: ViewCreationContext): UserByNameViewRouter =
    new UserByNameViewRouter(viewFactory(context))

  override final def additionalDescriptors: Seq[Descriptors.FileDescriptor] =
    ExampleViewsProto.javaDescriptor ::
    Nil
}
