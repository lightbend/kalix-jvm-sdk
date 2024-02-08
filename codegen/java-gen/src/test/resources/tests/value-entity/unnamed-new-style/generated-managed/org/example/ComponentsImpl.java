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
  public Components.CounterServiceEntityCalls counterServiceEntity() {
    return new CounterServiceEntityCallsImpl();
  }

  private final class CounterServiceEntityCallsImpl implements Components.CounterServiceEntityCalls {
     @Override
    public DeferredCall<org.example.valueentity.CounterApi.IncreaseValue, com.google.protobuf.Empty> increase(org.example.valueentity.CounterApi.IncreaseValue increaseValue) {
      return new GrpcDeferredCall<>(
        increaseValue,
        context.componentCallMetadata(),
        "org.example.valueentity.CounterService",
        "Increase",
        (Metadata metadata) -> {
          org.example.valueentity.CounterService client = getGrpcClient(org.example.valueentity.CounterService.class);
          if (client instanceof org.example.valueentity.CounterServiceClient) {
            return addHeaders(((org.example.valueentity.CounterServiceClient) client).increase(), metadata).invoke(increaseValue);
          } else {
            // only for tests with mocked client implementation
            return client.increase(increaseValue);
          }
        }
      );
    }
    @Override
    public DeferredCall<org.example.valueentity.CounterApi.DecreaseValue, com.google.protobuf.Empty> decrease(org.example.valueentity.CounterApi.DecreaseValue decreaseValue) {
      return new GrpcDeferredCall<>(
        decreaseValue,
        context.componentCallMetadata(),
        "org.example.valueentity.CounterService",
        "Decrease",
        (Metadata metadata) -> {
          org.example.valueentity.CounterService client = getGrpcClient(org.example.valueentity.CounterService.class);
          if (client instanceof org.example.valueentity.CounterServiceClient) {
            return addHeaders(((org.example.valueentity.CounterServiceClient) client).decrease(), metadata).invoke(decreaseValue);
          } else {
            // only for tests with mocked client implementation
            return client.decrease(decreaseValue);
          }
        }
      );
    }
  }
}
