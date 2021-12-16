package org.example.service;

import com.akkaserverless.javasdk.action.ActionCreationContext;
import com.google.protobuf.Empty;

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

/** An action. */
public class MyServiceActionImpl extends AbstractMyServiceAction {

  public MyServiceActionImpl(ActionCreationContext creationContext) {}

  /** Handler for "simpleMethod". */
  @Override
  public Effect<Empty> simpleMethod(ServiceOuterClass.MyRequest myRequest) {
    throw new RuntimeException("The command handler for `simpleMethod` is not implemented, yet");
  }
}
