package org.example.service;

import com.google.protobuf.Empty;
import org.example.Components;
import org.example.ComponentsImpl;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public abstract class AbstractMyServiceAction extends kalix.javasdk.action.Action {

  protected final Components components() {
    return new ComponentsImpl(contextForComponents());
  }

  public abstract Effect<Empty> simpleMethod(ServiceOuterClass.MyRequest myRequest);
}