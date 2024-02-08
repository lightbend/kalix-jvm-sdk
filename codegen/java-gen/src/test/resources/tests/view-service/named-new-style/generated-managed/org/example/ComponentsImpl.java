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
  public Components.MyUserByNameViewCalls myUserByNameView() {
    return new MyUserByNameViewCallsImpl();
  }

  private final class MyUserByNameViewCallsImpl implements Components.MyUserByNameViewCalls {
     @Override
    public DeferredCall<org.example.named.view.UserViewModel.ByNameRequest, org.example.named.view.UserViewModel.UserResponse> getUserByName(org.example.named.view.UserViewModel.ByNameRequest byNameRequest) {
      return new GrpcDeferredCall<>(
        byNameRequest,
        context.componentCallMetadata(),
        "org.example.named.view.UserByName",
        "GetUserByName",
        (Metadata metadata) -> {
          org.example.named.view.UserByName client = getGrpcClient(org.example.named.view.UserByName.class);
          if (client instanceof org.example.named.view.UserByNameClient) {
            return addHeaders(((org.example.named.view.UserByNameClient) client).getUserByName(), metadata).invoke(byNameRequest);
          } else {
            // only for tests with mocked client implementation
            return client.getUserByName(byNameRequest);
          }
        }
      );
    }
  }
}
