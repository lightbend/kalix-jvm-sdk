package org.example.service

import com.google.protobuf.Descriptors
import kalix.scalasdk.action.ActionCreationContext
import kalix.scalasdk.action.ActionOptions
import kalix.scalasdk.action.ActionProvider

import scala.collection.immutable.Seq

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object MyServiceNamedActionProvider {
  def apply(actionFactory: ActionCreationContext => MyServiceNamedAction): MyServiceNamedActionProvider =
    new MyServiceNamedActionProvider(actionFactory, ActionOptions.defaults)

  def apply(actionFactory: ActionCreationContext => MyServiceNamedAction, options: ActionOptions): MyServiceNamedActionProvider =
    new MyServiceNamedActionProvider(actionFactory, options)
}

class MyServiceNamedActionProvider private(actionFactory: ActionCreationContext => MyServiceNamedAction,
                                      override val options: ActionOptions)
  extends ActionProvider[MyServiceNamedAction] {

  override final def serviceDescriptor: Descriptors.ServiceDescriptor =
    ExampleActionProto.javaDescriptor.findServiceByName("MyService")

  override final def newRouter(context: ActionCreationContext): MyServiceNamedActionRouter =
    new MyServiceNamedActionRouter(actionFactory(context))

  override final def additionalDescriptors: Seq[Descriptors.FileDescriptor] =
    ExampleActionProto.javaDescriptor ::
    Nil

  def withOptions(options: ActionOptions): MyServiceNamedActionProvider =
    new MyServiceNamedActionProvider(actionFactory, options)
}

