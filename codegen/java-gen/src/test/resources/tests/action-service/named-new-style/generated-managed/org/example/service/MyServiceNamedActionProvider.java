package org.example.service;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Empty;
import com.google.protobuf.EmptyProto;
import kalix.javasdk.action.ActionCreationContext;
import kalix.javasdk.action.ActionOptions;
import kalix.javasdk.action.ActionProvider;
import kalix.javasdk.impl.action.ActionRouter;

import java.util.function.Function;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * MyServiceNamedActionProvider that defines how to register and create the action for
 * the Protobuf service <code>MyService</code>.
 *
 * Should be used with the <code>register</code> method in {@link kalix.javasdk.Kalix}.
 */
public class MyServiceNamedActionProvider implements ActionProvider<MyServiceNamedAction> {

  private final Function<ActionCreationContext, MyServiceNamedAction> actionFactory;
  private final ActionOptions options;

  /** Factory method of MyServiceNamedActionProvider */
  public static MyServiceNamedActionProvider of(Function<ActionCreationContext, MyServiceNamedAction> actionFactory) {
    return new MyServiceNamedActionProvider(actionFactory, ActionOptions.defaults());
  }

  private MyServiceNamedActionProvider(Function<ActionCreationContext, MyServiceNamedAction> actionFactory, ActionOptions options) {
    this.actionFactory = actionFactory;
    this.options = options;
  }

  @Override
  public final ActionOptions options() {
    return options;
  }

  public final MyServiceNamedActionProvider withOptions(ActionOptions options) {
    return new MyServiceNamedActionProvider(actionFactory, options);
  }

  @Override
  public final Descriptors.ServiceDescriptor serviceDescriptor() {
    return ServiceOuterClass.getDescriptor().findServiceByName("MyService");
  }

  @Override
  public final MyServiceNamedActionRouter newRouter(ActionCreationContext context) {
    return new MyServiceNamedActionRouter(actionFactory.apply(context));
  }

  @Override
  public final Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {
      EmptyProto.getDescriptor(),
      ServiceOuterClass.getDescriptor()
    };
  }

}
