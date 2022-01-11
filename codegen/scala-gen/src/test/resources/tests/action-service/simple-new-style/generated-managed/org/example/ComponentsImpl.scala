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
 override def myServiceAction: Components.MyServiceActionCalls =
   new MyServiceActionCallsImpl();


 private final class MyServiceActionCallsImpl extends Components.MyServiceActionCalls {
   override def simpleMethod(command: _root_.org.example.service.example_action.MyRequest): DeferredCall[_root_.org.example.service.example_action.MyRequest, _root_.org.external.external_domain.Empty] =
     ScalaDeferredCallAdapter(
       command,
       Metadata.empty,
       "org.example.service.MyService",
       "simpleMethod",
       () => getGrpcClient(classOf[_root_.org.example.service.example_action.MyService]).simpleMethod(command)
     )
 }

}
