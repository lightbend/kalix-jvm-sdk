package org.example.view.example_views

import com.akkaserverless.javasdk.impl.view.UpdateHandlerNotFound
import com.akkaserverless.scalasdk.impl.view.ViewRouter
import com.akkaserverless.scalasdk.view.View
import com.akkaserverless.scalasdk.view.ViewCreationContext
import com.akkaserverless.scalasdk.view.ViewOptions
import com.akkaserverless.scalasdk.view.ViewProvider
import com.google.protobuf.Descriptors
import com.google.protobuf.EmptyProto

import scala.collection.immutable.Seq

// This code is managed by Akka Serverless tooling.
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
  extends ViewProvider[UserState, UserByNameViewImpl] {

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
