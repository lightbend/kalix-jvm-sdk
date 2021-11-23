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
  public Components.MyServiceActionCalls myServiceAction() {
    return new MyServiceActionCallsImpl();
  }

  private final class MyServiceActionCallsImpl implements Components.MyServiceActionCalls {
     @Override
    public DeferredCall<com.example.service.ServiceOuterClass.MyRequest, com.google.protobuf.Empty> simpleMethod(com.example.service.ServiceOuterClass.MyRequest myRequest) {
      return new DeferredCallImpl<>(
        myRequest,
        MetadataImpl.Empty(),
        "com.example.service.MyService",
        "simpleMethod",
        () -> getGrpcClient(com.example.service.MyService.class).simpleMethod(myRequest)
      );
    }
  }
}
