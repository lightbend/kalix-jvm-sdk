package org.example.named.view

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

object MyUserByNameViewProvider {
  def apply(viewFactory: ViewCreationContext => MyUserByNameView): MyUserByNameViewProvider =
    new MyUserByNameViewProvider(viewFactory, viewId = "UserByName", options = ViewOptions.defaults)
}

class MyUserByNameViewProvider private(
    viewFactory: ViewCreationContext => MyUserByNameView,
    override val viewId: String,
    override val options: ViewOptions)
  extends ViewProvider {

  /**
   * Use a custom view identifier. By default, the viewId is the same as the proto service name.
   * A different identifier can be needed when making rolling updates with changes to the view definition.
   */
  def withViewId(viewId: String): MyUserByNameViewProvider =
    new MyUserByNameViewProvider(viewFactory, viewId, options)

  def withOptions(newOptions: ViewOptions): MyUserByNameViewProvider =
    new MyUserByNameViewProvider(viewFactory, viewId, newOptions)

  override final def serviceDescriptor: Descriptors.ServiceDescriptor =
    ExampleNamedViewsProto.javaDescriptor.findServiceByName("UserByName")

  override final def newRouter(context: ViewCreationContext): MyUserByNameViewRouter =
    new MyUserByNameViewRouter(viewFactory(context))

  override final def additionalDescriptors: Seq[Descriptors.FileDescriptor] =
    ExampleNamedViewsProto.javaDescriptor ::
    Nil
}
