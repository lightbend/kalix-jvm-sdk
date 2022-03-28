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
 override def myServiceAction: Components.MyServiceActionCalls =
   new MyServiceActionCallsImpl();


 private final class MyServiceActionCallsImpl extends Components.MyServiceActionCalls {
   override def simpleMethod(command: _root_.org.example.service.MyRequest): DeferredCall[_root_.org.example.service.MyRequest, _root_.org.external.Empty] =
     ScalaDeferredCallAdapter(
       command,
       Metadata.empty,
       "org.example.service.MyService",
       "simpleMethod",
       () => getGrpcClient(classOf[_root_.org.example.service.MyService]).simpleMethod(command)
     )
 }

}
