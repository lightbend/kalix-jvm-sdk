package org.example;

import com.akkaserverless.javasdk.DeferredCall;

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * Not intended for user extension, provided through generated implementation
 */
public interface Components {
  MyServiceActionCalls myServiceAction();

  interface MyServiceActionCalls {
    DeferredCall<com.example.service.ServiceOuterClass.MyRequest, com.google.protobuf.Empty> simpleMethod(com.example.service.ServiceOuterClass.MyRequest myRequest);
  }
}
