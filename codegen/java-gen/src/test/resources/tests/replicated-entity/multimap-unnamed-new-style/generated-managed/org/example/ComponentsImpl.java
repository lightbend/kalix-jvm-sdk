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
  public Components.MultiMapServiceEntityCalls multiMapServiceEntity() {
    return new MultiMapServiceEntityCallsImpl();
  }

  private final class MultiMapServiceEntityCallsImpl implements Components.MultiMapServiceEntityCalls {
     @Override
    public DeferredCall<com.example.replicated.multimap.SomeMultiMapApi.PutValue, com.google.protobuf.Empty> put(com.example.replicated.multimap.SomeMultiMapApi.PutValue putValue) {
      return new GrpcDeferredCall<>(
        putValue,
        context.componentCallMetadata(),
        "com.example.replicated.multimap.MultiMapService",
        "Put",
        (Metadata metadata) -> {
          com.example.replicated.multimap.MultiMapService client = getGrpcClient(com.example.replicated.multimap.MultiMapService.class);
          if (client instanceof com.example.replicated.multimap.MultiMapServiceClient) {
            return addHeaders(((com.example.replicated.multimap.MultiMapServiceClient) client).put(), metadata).invoke(putValue);
          } else {
            // only for tests with mocked client implementation
            return client.put(putValue);
          }
        }
      );
    }
  }
}
