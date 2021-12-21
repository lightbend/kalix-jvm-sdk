package org.example.service;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.akkaserverless.javasdk.action.ActionCreationContext;
import org.external.ExternalDomain;

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

/** An action. */
public class MyServiceAction extends AbstractMyServiceAction {

  public MyServiceAction(ActionCreationContext creationContext) {}

  /** Handler for "simpleMethod". */
  @Override
  public Effect<ExternalDomain.Empty> simpleMethod(ServiceOuterClass.MyRequest myRequest) {
    throw new RuntimeException("The command handler for `simpleMethod` is not implemented, yet");
  }

  /** Handler for "streamedOutputMethod". */
  @Override
  public Source<Effect<ExternalDomain.Empty>, NotUsed> streamedOutputMethod(ServiceOuterClass.MyRequest myRequest) {
    throw new RuntimeException("The command handler for `streamedOutputMethod` is not implemented, yet");
  }

  /** Handler for "streamedInputMethod". */
  @Override
  public Effect<ExternalDomain.Empty> streamedInputMethod(Source<ServiceOuterClass.MyRequest, NotUsed> myRequestSrc) {
    throw new RuntimeException("The command handler for `streamedInputMethod` is not implemented, yet");
  }

  /** Handler for "fullStreamedMethod". */
  @Override
  public Source<Effect<ExternalDomain.Empty>, NotUsed> fullStreamedMethod(Source<ServiceOuterClass.MyRequest, NotUsed> myRequestSrc) {
    throw new RuntimeException("The command handler for `fullStreamedMethod` is not implemented, yet");
  }
}
