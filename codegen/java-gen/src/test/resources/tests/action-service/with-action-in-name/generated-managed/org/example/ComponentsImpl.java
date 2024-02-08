package org.example;

import akka.grpc.javadsl.SingleResponseRequestBuilder;
import kalix.javasdk.Context;
import kalix.javasdk.DeferredCall;
import kalix.javasdk.Metadata;
import kalix.javasdk.impl.GrpcDeferredCall;
import kalix.javasdk.impl.InternalContext;
import kalix.javasdk.impl.MetadataImpl;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * Not intended for direct instantiation, called by generated code, use Action.components() to access
 */
public final class ComponentsImpl implements Components {

  private final InternalContext context;

  public ComponentsImpl(Context context) {
    this.context = (InternalContext) context;
  }

  private <T> T getGrpcClient(Class<T> serviceClass) {
    return context.getComponentGrpcClient(serviceClass);
  }

  private <Req, Res> SingleResponseRequestBuilder<Req, Res> addHeaders(SingleResponseRequestBuilder<Req, Res> requestBuilder, Metadata metadata){
    SingleResponseRequestBuilder<Req, Res> updatedBuilder = requestBuilder;
    for (Metadata.MetadataEntry entry: metadata){
      if (entry.isText()) {
        updatedBuilder = updatedBuilder.addHeader(entry.getKey(), entry.getValue());
      }
    }
    return updatedBuilder;
  }

  @Override
  public Components.MyServiceActionImplCalls myServiceActionImpl() {
    return new MyServiceActionImplCallsImpl();
  }

  private final class MyServiceActionImplCallsImpl implements Components.MyServiceActionImplCalls {
     @Override
    public DeferredCall<org.example.service.ServiceOuterClass.MyRequest, com.google.protobuf.Empty> simpleMethod(org.example.service.ServiceOuterClass.MyRequest myRequest) {
      return new GrpcDeferredCall<>(
        myRequest,
        context.componentCallMetadata(),
        "org.example.service.MyServiceAction",
        "simpleMethod",
        (Metadata metadata) -> {
          org.example.service.MyServiceAction client = getGrpcClient(org.example.service.MyServiceAction.class);
          if (client instanceof org.example.service.MyServiceActionClient) {
            return addHeaders(((org.example.service.MyServiceActionClient) client).simpleMethod(), metadata).invoke(myRequest);
          } else {
            // only for tests with mocked client implementation
            return client.simpleMethod(myRequest);
          }
        }
      );
    }
  }
}
