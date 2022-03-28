package org.example

import kalix.scalasdk.Context
import kalix.scalasdk.DeferredCall
import kalix.scalasdk.Metadata
import kalix.scalasdk.impl.InternalContext
import kalix.scalasdk.impl.ScalaDeferredCallAdapter


// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * Not intended for direct instantiation, called by generated code, use Action.components() to access
 */
final class ComponentsImpl(context: InternalContext) extends Components {

  def this(context: Context) =
    this(context.asInstanceOf[InternalContext])

  private def getGrpcClient[T](serviceClass: Class[T]): T =
    context.getComponentGrpcClient(serviceClass)

 @Override
 override def myUserByNameView: Components.MyUserByNameViewCalls =
   new MyUserByNameViewCallsImpl();


 private final class MyUserByNameViewCallsImpl extends Components.MyUserByNameViewCalls {
   override def getUserByName(command: _root_.org.example.named.view.ByNameRequest): DeferredCall[_root_.org.example.named.view.ByNameRequest, _root_.org.example.named.view.UserResponse] =
     ScalaDeferredCallAdapter(
       command,
       Metadata.empty,
       "org.example.named.view.UserByName",
       "GetUserByName",
       () => getGrpcClient(classOf[_root_.org.example.named.view.UserByName]).getUserByName(command)
     )
 }

}
