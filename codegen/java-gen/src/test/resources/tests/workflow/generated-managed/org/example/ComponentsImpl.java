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
  public Components.SomeServiceActionCalls someServiceAction() {
    return new SomeServiceActionCallsImpl();
  }
  @Override
  public Components.TransferWorkflowCalls transferWorkflow() {
    return new TransferWorkflowCallsImpl();
  }

  private final class SomeServiceActionCallsImpl implements Components.SomeServiceActionCalls {
     @Override
    public DeferredCall<org.example.service.SomeServiceOuterClass.SomeRequest, com.google.protobuf.Empty> simpleMethod(org.example.service.SomeServiceOuterClass.SomeRequest someRequest) {
      return new GrpcDeferredCall<>(
        someRequest,
        context.componentCallMetadata(),
        "org.example.service.SomeService",
        "simpleMethod",
        (Metadata metadata) -> {
          org.example.service.SomeService client = getGrpcClient(org.example.service.SomeService.class);
          if (client instanceof org.example.service.SomeServiceClient) {
            return addHeaders(((org.example.service.SomeServiceClient) client).simpleMethod(), metadata).invoke(someRequest);
          } else {
            // only for tests with mocked client implementation
            return client.simpleMethod(someRequest);
          }
        }
      );
    }
  }
  private final class TransferWorkflowCallsImpl implements Components.TransferWorkflowCalls {
     @Override
    public DeferredCall<org.example.workflow.TransferWorkflowApi.Transfer, com.google.protobuf.Empty> start(org.example.workflow.TransferWorkflowApi.Transfer transfer) {
      return new GrpcDeferredCall<>(
        transfer,
        context.componentCallMetadata(),
        "org.example.workflow.TransferWorkflowService",
        "Start",
        (Metadata metadata) -> {
          org.example.workflow.TransferWorkflowService client = getGrpcClient(org.example.workflow.TransferWorkflowService.class);
          if (client instanceof org.example.workflow.TransferWorkflowServiceClient) {
            return addHeaders(((org.example.workflow.TransferWorkflowServiceClient) client).start(), metadata).invoke(transfer);
          } else {
            // only for tests with mocked client implementation
            return client.start(transfer);
          }
        }
      );
    }
    @Override
    public DeferredCall<com.google.protobuf.Empty, org.example.workflow.TransferWorkflowApi.Transfer> getState(com.google.protobuf.Empty empty) {
      return new GrpcDeferredCall<>(
        empty,
        context.componentCallMetadata(),
        "org.example.workflow.TransferWorkflowService",
        "GetState",
        (Metadata metadata) -> {
          org.example.workflow.TransferWorkflowService client = getGrpcClient(org.example.workflow.TransferWorkflowService.class);
          if (client instanceof org.example.workflow.TransferWorkflowServiceClient) {
            return addHeaders(((org.example.workflow.TransferWorkflowServiceClient) client).getState(), metadata).invoke(empty);
          } else {
            // only for tests with mocked client implementation
            return client.getState(empty);
          }
        }
      );
    }
  }
}
