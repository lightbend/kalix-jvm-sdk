package org.example;

import com.akkaserverless.javasdk.Context;
import com.akkaserverless.javasdk.DeferredCall;
import com.akkaserverless.javasdk.impl.DeferredCallImpl;
import com.akkaserverless.javasdk.impl.InternalContext;
import com.akkaserverless.javasdk.impl.MetadataImpl;

// This code is managed by Akka Serverless tooling.
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

  @Override
  public Components.UserByNameViewImplCalls userByNameViewImpl() {
    return new UserByNameViewImplCallsImpl();
  }

  private final class UserByNameViewImplCallsImpl implements Components.UserByNameViewImplCalls {
     @Override
    public DeferredCall<org.example.view.UserViewModel.ByNameRequest, org.example.view.UserViewModel.UserResponse> getUserByName(org.example.view.UserViewModel.ByNameRequest byNameRequest) {
      return new DeferredCallImpl<>(
        byNameRequest,
        MetadataImpl.Empty(),
        "org.example.view.UserByNameView",
        "GetUserByName",
        () -> getGrpcClient(org.example.view.UserByNameView.class).getUserByName(byNameRequest)
      );
    }
  }
}
