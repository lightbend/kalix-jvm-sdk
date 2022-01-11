package org.example

import com.akkaserverless.scalasdk.Context
import com.akkaserverless.scalasdk.DeferredCall
import com.akkaserverless.scalasdk.Metadata
import com.akkaserverless.scalasdk.impl.InternalContext
import com.akkaserverless.scalasdk.impl.ScalaDeferredCallAdapter


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
   override def getUserByName(command: _root_.org.example.named.view.example_named_views.ByNameRequest): DeferredCall[_root_.org.example.named.view.example_named_views.ByNameRequest, _root_.org.example.named.view.example_named_views.UserResponse] =
     ScalaDeferredCallAdapter(
       command,
       Metadata.empty,
       "org.example.named.view.UserByName",
       "GetUserByName",
       () => getGrpcClient(classOf[_root_.org.example.named.view.example_named_views.UserByName]).getUserByName(command)
     )
 }

}
