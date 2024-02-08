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
 override def myUserByNameView: Components.MyUserByNameViewCalls =
   new MyUserByNameViewCallsImpl();


 private final class MyUserByNameViewCallsImpl extends Components.MyUserByNameViewCalls {
   override def getUserByName(command: _root_.org.example.named.view.ByNameRequest): DeferredCall[_root_.org.example.named.view.ByNameRequest, _root_.org.example.named.view.UserResponse] =
     ScalaDeferredCallAdapter(
       command,
       context.componentCallMetadata,
       "org.example.named.view.UserByName",
       "GetUserByName",
       (metadata: Metadata) => {
         val client = getGrpcClient(classOf[_root_.org.example.named.view.UserByName])
         if (client.isInstanceOf[_root_.org.example.named.view.UserByNameClient]) {
           addHeaders(
             client.asInstanceOf[_root_.org.example.named.view.UserByNameClient].getUserByName(),
             metadata).invoke(command)
         } else {
           // only for tests with mocked client implementation
           client.getUserByName(command)
         }
       })
 }

}
