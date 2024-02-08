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
  public Components.UserByNameViewImplCalls userByNameViewImpl() {
    return new UserByNameViewImplCallsImpl();
  }

  private final class UserByNameViewImplCallsImpl implements Components.UserByNameViewImplCalls {
     @Override
    public DeferredCall<org.example.view.UserViewModel.ByNameRequest, org.example.view.UserViewModel.UserResponse> getUserByName(org.example.view.UserViewModel.ByNameRequest byNameRequest) {
      return new GrpcDeferredCall<>(
        byNameRequest,
        context.componentCallMetadata(),
        "org.example.view.UserByNameView",
        "GetUserByName",
        (Metadata metadata) -> {
          org.example.view.UserByNameView client = getGrpcClient(org.example.view.UserByNameView.class);
          if (client instanceof org.example.view.UserByNameViewClient) {
            return addHeaders(((org.example.view.UserByNameViewClient) client).getUserByName(), metadata).invoke(byNameRequest);
          } else {
            // only for tests with mocked client implementation
            return client.getUserByName(byNameRequest);
          }
        }
      );
    }
  }
}
