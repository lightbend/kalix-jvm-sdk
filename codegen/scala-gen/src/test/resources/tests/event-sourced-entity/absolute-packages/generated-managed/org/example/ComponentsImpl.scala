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
 override def counter: Components.CounterCalls =
   new CounterCallsImpl();


 private final class CounterCallsImpl extends Components.CounterCalls {
   override def increase(command: _root_.org.example.eventsourcedentity.IncreaseValue): DeferredCall[_root_.org.example.eventsourcedentity.IncreaseValue, _root_.com.google.protobuf.empty.Empty] =
     ScalaDeferredCallAdapter(
       command,
       Metadata.empty,
       "org.example.eventsourcedentity.CounterService",
       "Increase",
       () => getGrpcClient(classOf[_root_.org.example.eventsourcedentity.CounterService]).increase(command)
     )
   override def decrease(command: _root_.org.example.eventsourcedentity.DecreaseValue): DeferredCall[_root_.org.example.eventsourcedentity.DecreaseValue, _root_.com.google.protobuf.empty.Empty] =
     ScalaDeferredCallAdapter(
       command,
       Metadata.empty,
       "org.example.eventsourcedentity.CounterService",
       "Decrease",
       () => getGrpcClient(classOf[_root_.org.example.eventsourcedentity.CounterService]).decrease(command)
     )
 }

}
