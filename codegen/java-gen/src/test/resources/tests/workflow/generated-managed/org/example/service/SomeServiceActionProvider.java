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
 * SomeServiceActionProvider that defines how to register and create the action for
 * the Protobuf service <code>SomeService</code>.
 *
 * Should be used with the <code>register</code> method in {@link kalix.javasdk.Kalix}.
 */
public class SomeServiceActionProvider implements ActionProvider<SomeServiceAction> {

  private final Function<ActionCreationContext, SomeServiceAction> actionFactory;
  private final ActionOptions options;

  /** Factory method of SomeServiceActionProvider */
  public static SomeServiceActionProvider of(Function<ActionCreationContext, SomeServiceAction> actionFactory) {
    return new SomeServiceActionProvider(actionFactory, ActionOptions.defaults());
  }

  private SomeServiceActionProvider(Function<ActionCreationContext, SomeServiceAction> actionFactory, ActionOptions options) {
    this.actionFactory = actionFactory;
    this.options = options;
  }

  @Override
  public final ActionOptions options() {
    return options;
  }

  public final SomeServiceActionProvider withOptions(ActionOptions options) {
    return new SomeServiceActionProvider(actionFactory, options);
  }

  @Override
  public final Descriptors.ServiceDescriptor serviceDescriptor() {
    return SomeServiceOuterClass.getDescriptor().findServiceByName("SomeService");
  }

  @Override
  public final SomeServiceActionRouter newRouter(ActionCreationContext context) {
    return new SomeServiceActionRouter(actionFactory.apply(context));
  }

  @Override
  public final Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {
      EmptyProto.getDescriptor(),
      SomeServiceOuterClass.getDescriptor()
    };
  }

}
