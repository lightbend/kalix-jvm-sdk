package org.example.service;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.google.protobuf.Empty;
import org.example.Components;
import org.example.ComponentsImpl;
import org.external.ExternalDomain;

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/** An action. */
public abstract class AbstractMyServiceAction extends com.akkaserverless.javasdk.action.Action {

  protected final Components components() {
    return new ComponentsImpl(actionContext());
  }

  /** Handler for "simpleMethod". */
  public abstract Effect<ExternalDomain.Empty> simpleMethod(ServiceOuterClass.MyRequest myRequest);

  /** Handler for "streamedOutputMethod". */
  public abstract Source<Effect<Empty>, NotUsed> streamedOutputMethod(ServiceOuterClass.MyRequest myRequest);

  /** Handler for "streamedInputMethod". */
  public abstract Effect<Empty> streamedInputMethod(Source<ServiceOuterClass.MyRequest, NotUsed> myRequestSrc);

  /** Handler for "fullStreamedMethod". */
  public abstract Source<Effect<Empty>, NotUsed> fullStreamedMethod(Source<ServiceOuterClass.MyRequest, NotUsed> myRequestSrc);
}