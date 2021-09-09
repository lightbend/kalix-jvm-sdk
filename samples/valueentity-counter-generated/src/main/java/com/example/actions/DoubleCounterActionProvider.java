/* This code is managed by Akka Serverless tooling.
 * It will be re-generated to reflect any changes to your protobuf definitions.
 * DO NOT EDIT
 */

package com.example.actions;

import com.akkaserverless.javasdk.action.ActionCreationContext;
import com.akkaserverless.javasdk.action.ActionProvider;
import com.akkaserverless.javasdk.impl.action.ActionHandler;
import com.example.CounterApi;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Empty;
import com.google.protobuf.EmptyProto;
import java.util.function.Function;

/**
 * DoubleCounterActionProvider that defines how to register and create the action for
 * the Protobuf service <code>DoubleCounter</code>.
 *
 * Should be used with the <code>register</code> method in {@link com.akkaserverless.javasdk.AkkaServerless}.
 */
public class DoubleCounterActionProvider implements ActionProvider {

  private final Function<ActionCreationContext, DoubleCounterAction> actionFactory;

  /** Factory method of DoubleCounterActionProvider */
  public static DoubleCounterActionProvider of(Function<ActionCreationContext, DoubleCounterAction> actionFactory) {
    return new DoubleCounterActionProvider(actionFactory);
  }

  private DoubleCounterActionProvider(Function<ActionCreationContext, DoubleCounterAction> actionFactory) {
    this.actionFactory = actionFactory;
  }

  @Override
  public final Descriptors.ServiceDescriptor serviceDescriptor() {
    return DoubleCounterApi.getDescriptor().findServiceByName("DoubleCounter");
  }

  @Override
  public final ActionHandler newHandler(ActionCreationContext context) {
    return new DoubleCounterActionHandler(actionFactory.apply(context));
  }

  @Override
  public final Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {
      CounterApi.getDescriptor(),
      DoubleCounterApi.getDescriptor(),
      EmptyProto.getDescriptor()
    };
  }

}