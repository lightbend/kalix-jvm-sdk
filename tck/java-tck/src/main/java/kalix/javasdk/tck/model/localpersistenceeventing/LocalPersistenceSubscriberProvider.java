/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.tck.model.localpersistenceeventing;

import kalix.javasdk.action.ActionCreationContext;
import kalix.javasdk.action.ActionOptions;
import kalix.javasdk.action.ActionProvider;
import kalix.tck.model.eventing.LocalPersistenceEventing;
import com.google.protobuf.Descriptors;

import java.util.function.Function;

public class LocalPersistenceSubscriberProvider
    implements ActionProvider<LocalPersistenceSubscriber> {

  private final Function<ActionCreationContext, LocalPersistenceSubscriber> actionFactory;
  private final ActionOptions options;

  private LocalPersistenceSubscriberProvider(
      Function<ActionCreationContext, LocalPersistenceSubscriber> actionFactory,
      ActionOptions options) {
    this.actionFactory = actionFactory;
    this.options = options;
  }

  public static LocalPersistenceSubscriberProvider of(
      Function<ActionCreationContext, LocalPersistenceSubscriber> actionFactory) {
    return new LocalPersistenceSubscriberProvider(actionFactory, ActionOptions.defaults());
  }

  @Override
  public final ActionOptions options() {
    return options;
  }

  public final LocalPersistenceSubscriberProvider withOptions(ActionOptions options) {
    return new LocalPersistenceSubscriberProvider(actionFactory, options);
  }

  @Override
  public Descriptors.ServiceDescriptor serviceDescriptor() {
    return LocalPersistenceEventing.getDescriptor()
        .findServiceByName("LocalPersistenceSubscriberModel");
  }

  @Override
  public LocalPersistenceSubscriberRouter newRouter(ActionCreationContext context) {
    return new LocalPersistenceSubscriberRouter(actionFactory.apply(context));
  }

  @Override
  public Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {LocalPersistenceEventing.getDescriptor()};
  }
}
