package org.example;

import kalix.javasdk.DeferredCall;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * Not intended for user extension, provided through generated implementation
 */
public interface Components {
  MyServiceActionCalls myServiceAction();

  interface MyServiceActionCalls {
    DeferredCall<org.example.service.ServiceOuterClass.MyRequest, org.external.ExternalDomain.Empty> simpleMethod(org.example.service.ServiceOuterClass.MyRequest myRequest);
  }
}
