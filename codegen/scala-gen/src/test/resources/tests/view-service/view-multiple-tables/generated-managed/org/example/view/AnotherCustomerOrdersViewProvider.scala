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

object AnotherCustomerOrdersViewProvider {
  def apply(viewFactory: ViewCreationContext => AnotherCustomerOrdersViewImpl): AnotherCustomerOrdersViewProvider =
    new AnotherCustomerOrdersViewProvider(viewFactory, viewId = "AnotherCustomerOrdersView", options = ViewOptions.defaults)
}

class AnotherCustomerOrdersViewProvider private(
    viewFactory: ViewCreationContext => AnotherCustomerOrdersViewImpl,
    override val viewId: String,
    override val options: ViewOptions)
  extends ViewProvider {

  /**
   * Use a custom view identifier. By default, the viewId is the same as the proto service name.
   * A different identifier can be needed when making rolling updates with changes to the view definition.
   */
  def withViewId(viewId: String): AnotherCustomerOrdersViewProvider =
    new AnotherCustomerOrdersViewProvider(viewFactory, viewId, options)

  def withOptions(newOptions: ViewOptions): AnotherCustomerOrdersViewProvider =
    new AnotherCustomerOrdersViewProvider(viewFactory, viewId, newOptions)

  override final def serviceDescriptor: Descriptors.ServiceDescriptor =
    CustomerOrdersProto.javaDescriptor.findServiceByName("AnotherCustomerOrdersView")

  override final def newRouter(context: ViewCreationContext): AnotherCustomerOrdersViewRouter =
    new AnotherCustomerOrdersViewRouter(viewFactory(context))

  override final def additionalDescriptors: Seq[Descriptors.FileDescriptor] =
    CustomerOrdersProto.javaDescriptor ::
    Nil
}
