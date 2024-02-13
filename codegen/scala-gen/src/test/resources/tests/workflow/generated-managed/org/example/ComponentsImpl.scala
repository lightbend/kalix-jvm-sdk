package org.example

import akka.grpc.scaladsl.SingleResponseRequestBuilder
import kalix.scalasdk.Context
import kalix.scalasdk.DeferredCall
import kalix.scalasdk.Metadata
import kalix.scalasdk.impl.InternalContext
import kalix.scalasdk.impl.ScalaDeferredCallAdapter


// This code is managed by Kalix tooling.
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

  private def addHeaders[Req, Res](
      requestBuilder: SingleResponseRequestBuilder[Req, Res],
      metadata: Metadata): SingleResponseRequestBuilder[Req, Res] = {
    metadata.filter(_.isText).foldLeft(requestBuilder) { (builder, entry) =>
      builder.addHeader(entry.key, entry.value)
    }
  }

 @Override
 override def someServiceAction: Components.SomeServiceActionCalls =
   new SomeServiceActionCallsImpl();

 @Override
 override def transferWorkflow: Components.TransferWorkflowCalls =
   new TransferWorkflowCallsImpl();


 private final class SomeServiceActionCallsImpl extends Components.SomeServiceActionCalls {
   override def simpleMethod(command: _root_.org.example.service.SomeRequest): DeferredCall[_root_.org.example.service.SomeRequest, _root_.com.google.protobuf.empty.Empty] =
     ScalaDeferredCallAdapter(
       command,
       context.componentCallMetadata,
       "org.example.service.SomeService",
       "simpleMethod",
       (metadata: Metadata) => {
         val client = getGrpcClient(classOf[_root_.org.example.service.SomeService])
         if (client.isInstanceOf[_root_.org.example.service.SomeServiceClient]) {
           addHeaders(
             client.asInstanceOf[_root_.org.example.service.SomeServiceClient].simpleMethod(),
             metadata).invoke(command)
         } else {
           // only for tests with mocked client implementation
           client.simpleMethod(command)
         }
       })
 }
 private final class TransferWorkflowCallsImpl extends Components.TransferWorkflowCalls {
   override def start(command: _root_.org.example.workflow.Transfer): DeferredCall[_root_.org.example.workflow.Transfer, _root_.com.google.protobuf.empty.Empty] =
     ScalaDeferredCallAdapter(
       command,
       context.componentCallMetadata,
       "org.example.workflow.TransferWorkflowService",
       "Start",
       (metadata: Metadata) => {
         val client = getGrpcClient(classOf[_root_.org.example.workflow.TransferWorkflowService])
         if (client.isInstanceOf[_root_.org.example.workflow.TransferWorkflowServiceClient]) {
           addHeaders(
             client.asInstanceOf[_root_.org.example.workflow.TransferWorkflowServiceClient].start(),
             metadata).invoke(command)
         } else {
           // only for tests with mocked client implementation
           client.start(command)
         }
       })
   override def getState(command: _root_.com.google.protobuf.empty.Empty): DeferredCall[_root_.com.google.protobuf.empty.Empty, _root_.org.example.workflow.Transfer] =
     ScalaDeferredCallAdapter(
       command,
       context.componentCallMetadata,
       "org.example.workflow.TransferWorkflowService",
       "GetState",
       (metadata: Metadata) => {
         val client = getGrpcClient(classOf[_root_.org.example.workflow.TransferWorkflowService])
         if (client.isInstanceOf[_root_.org.example.workflow.TransferWorkflowServiceClient]) {
           addHeaders(
             client.asInstanceOf[_root_.org.example.workflow.TransferWorkflowServiceClient].getState(),
             metadata).invoke(command)
         } else {
           // only for tests with mocked client implementation
           client.getState(command)
         }
       })
 }

}
