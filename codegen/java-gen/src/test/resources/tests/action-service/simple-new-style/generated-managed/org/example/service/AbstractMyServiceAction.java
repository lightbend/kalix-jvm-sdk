package org.example.service;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import org.example.Components;
import org.example.ComponentsImpl;
import org.external.ExternalDomain;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public abstract class AbstractMyServiceAction extends kalix.javasdk.action.Action {

  protected final Components components() {
    return new ComponentsImpl(contextForComponents());
  }

  public abstract Effect<ExternalDomain.Empty> simpleMethod(ServiceOuterClass.MyRequest myRequest);

  public abstract Source<Effect<ExternalDomain.Empty>, NotUsed> streamedOutputMethod(ServiceOuterClass.MyRequest myRequest);

  public abstract Effect<ExternalDomain.Empty> streamedInputMethod(Source<ServiceOuterClass.MyRequest, NotUsed> myRequestSrc);

  public abstract Source<Effect<ExternalDomain.Empty>, NotUsed> fullStreamedMethod(Source<ServiceOuterClass.MyRequest, NotUsed> myRequestSrc);
}