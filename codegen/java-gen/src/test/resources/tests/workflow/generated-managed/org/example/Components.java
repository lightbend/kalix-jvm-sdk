package org.example;

import kalix.javasdk.DeferredCall;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * Not intended for user extension, provided through generated implementation
 */
public interface Components {
  SomeServiceActionCalls someServiceAction();
  TransferWorkflowCalls transferWorkflow();

  interface SomeServiceActionCalls {
    DeferredCall<org.example.service.SomeServiceOuterClass.SomeRequest, com.google.protobuf.Empty> simpleMethod(org.example.service.SomeServiceOuterClass.SomeRequest someRequest);
  }
  interface TransferWorkflowCalls {
    DeferredCall<org.example.workflow.TransferWorkflowApi.Transfer, com.google.protobuf.Empty> start(org.example.workflow.TransferWorkflowApi.Transfer transfer);

    DeferredCall<com.google.protobuf.Empty, org.example.workflow.TransferWorkflowApi.Transfer> getState(com.google.protobuf.Empty empty);
  }
}
