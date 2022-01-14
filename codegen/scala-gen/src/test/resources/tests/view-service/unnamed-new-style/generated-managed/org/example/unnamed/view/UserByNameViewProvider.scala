package org.example.unnamed.view

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
  def apply(viewFactory: ViewCreationContext => UserByNameView): UserByNameViewProvider =
    new UserByNameViewProvider(viewFactory, viewId = "UserByName", options = ViewOptions.defaults)
}

class UserByNameViewProvider private(
    viewFactory: ViewCreationContext => UserByNameView,
    override val viewId: String,
    override val options: ViewOptions)
  extends ViewProvider[UserState, UserByNameView] {

  /**
   * Use a custom view identifier. By default, the viewId is the same as the proto service name.
   * A different identifier can be needed when making rolling updates with changes to the view definition.
   */
  def withViewId(viewId: String): UserByNameViewProvider =
    new UserByNameViewProvider(viewFactory, viewId, options)

  def withOptions(newOptions: ViewOptions): UserByNameViewProvider =
    new UserByNameViewProvider(viewFactory, viewId, newOptions)

  override final def serviceDescriptor: Descriptors.ServiceDescriptor =
    ExampleUnnamedViewsProto.javaDescriptor.findServiceByName("UserByName")

  override final def newRouter(context: ViewCreationContext): UserByNameViewRouter =
    new UserByNameViewRouter(viewFactory(context))

  override final def additionalDescriptors: Seq[Descriptors.FileDescriptor] =
    ExampleUnnamedViewsProto.javaDescriptor ::
    Nil
}
