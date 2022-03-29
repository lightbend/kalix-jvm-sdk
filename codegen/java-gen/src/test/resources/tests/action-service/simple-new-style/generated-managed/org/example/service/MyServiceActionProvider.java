package org.example.service;

import com.google.protobuf.Descriptors;
import kalix.javasdk.action.ActionCreationContext;
import kalix.javasdk.action.ActionOptions;
import kalix.javasdk.action.ActionProvider;
import kalix.javasdk.impl.action.ActionRouter;
import org.external.ExternalDomain;

import java.util.function.Function;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * MyServiceActionProvider that defines how to register and create the action for
 * the Protobuf service <code>MyService</code>.
 *
 * Should be used with the <code>register</code> method in {@link kalix.javasdk.Kalix}.
 */
public class MyServiceActionProvider implements ActionProvider<MyServiceAction> {

  private final Function<ActionCreationContext, MyServiceAction> actionFactory;
  private final ActionOptions options;

  /** Factory method of MyServiceActionProvider */
  public static MyServiceActionProvider of(Function<ActionCreationContext, MyServiceAction> actionFactory) {
    return new MyServiceActionProvider(actionFactory, ActionOptions.defaults());
  }

  private MyServiceActionProvider(Function<ActionCreationContext, MyServiceAction> actionFactory, ActionOptions options) {
    this.actionFactory = actionFactory;
    this.options = options;
  }

  @Override
  public final ActionOptions options() {
    return options;
  }

  public final MyServiceActionProvider withOptions(ActionOptions options) {
    return new MyServiceActionProvider(actionFactory, options);
  }

  @Override
  public final Descriptors.ServiceDescriptor serviceDescriptor() {
    return ServiceOuterClass.getDescriptor().findServiceByName("MyService");
  }

  @Override
  public final MyServiceActionRouter newRouter(ActionCreationContext context) {
    return new MyServiceActionRouter(actionFactory.apply(context));
  }

  @Override
  public final Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {
      ExternalDomain.getDescriptor(),
      ServiceOuterClass.getDescriptor()
    };
  }

}
