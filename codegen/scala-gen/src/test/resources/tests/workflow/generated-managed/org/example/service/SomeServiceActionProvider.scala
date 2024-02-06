package org.example.service

import com.google.protobuf.Descriptors
import kalix.scalasdk.action.ActionCreationContext
import kalix.scalasdk.action.ActionOptions
import kalix.scalasdk.action.ActionProvider

import scala.collection.immutable.Seq

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object SomeServiceActionProvider {
  def apply(actionFactory: ActionCreationContext => SomeServiceAction): SomeServiceActionProvider =
    new SomeServiceActionProvider(actionFactory, ActionOptions.defaults)

  def apply(actionFactory: ActionCreationContext => SomeServiceAction, options: ActionOptions): SomeServiceActionProvider =
    new SomeServiceActionProvider(actionFactory, options)
}

class SomeServiceActionProvider private(actionFactory: ActionCreationContext => SomeServiceAction,
                                      override val options: ActionOptions)
  extends ActionProvider[SomeServiceAction] {

  override final def serviceDescriptor: Descriptors.ServiceDescriptor =
    SomeActionProto.javaDescriptor.findServiceByName("SomeService")

  override final def newRouter(context: ActionCreationContext): SomeServiceActionRouter =
    new SomeServiceActionRouter(actionFactory(context))

  override final def additionalDescriptors: Seq[Descriptors.FileDescriptor] =
    SomeActionProto.javaDescriptor ::
    Nil

  def withOptions(options: ActionOptions): SomeServiceActionProvider =
    new SomeServiceActionProvider(actionFactory, options)
}

