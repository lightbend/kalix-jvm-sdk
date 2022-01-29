package org.example.service;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.google.protobuf.Empty;
import org.example.Components;
import org.example.ComponentsImpl;

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public abstract class AbstractMyServiceNamedAction extends com.akkaserverless.javasdk.action.Action {

  protected final Components components() {
    return new ComponentsImpl(actionContext());
  }

  public abstract Effect<Empty> simpleMethod(ServiceOuterClass.MyRequest myRequest);

  public abstract Source<Effect<Empty>, NotUsed> streamedOutputMethod(ServiceOuterClass.MyRequest myRequest);

  public abstract Effect<Empty> streamedInputMethod(Source<ServiceOuterClass.MyRequest, NotUsed> myRequestSrc);

  public abstract Source<Effect<Empty>, NotUsed> fullStreamedMethod(Source<ServiceOuterClass.MyRequest, NotUsed> myRequestSrc);
}