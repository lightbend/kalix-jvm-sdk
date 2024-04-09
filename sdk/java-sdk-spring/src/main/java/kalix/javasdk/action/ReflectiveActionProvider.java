/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.action;

import com.google.protobuf.Descriptors;
import kalix.javasdk.common.ForwardHeadersExtractor;
import kalix.javasdk.impl.MessageCodec;
import kalix.javasdk.impl.action.ActionRouter;
import kalix.javasdk.impl.ComponentDescriptor;
import kalix.javasdk.impl.ComponentDescriptorFactory;
import kalix.javasdk.impl.JsonMessageCodec;
import kalix.javasdk.impl.action.ReflectiveActionRouter;

import java.util.Optional;
import java.util.function.Function;

public class ReflectiveActionProvider<A extends Action> implements ActionProvider<A> {

  private final Function<ActionCreationContext, A> factory;

  private final ActionOptions options;
  private final Descriptors.FileDescriptor fileDescriptor;
  private final Descriptors.ServiceDescriptor serviceDescriptor;
  private final ComponentDescriptor componentDescriptor;
  private final JsonMessageCodec messageCodec;

  public static <A extends Action> ReflectiveActionProvider<A> of(
      Class<A> cls,
      JsonMessageCodec messageCodec,
      Function<ActionCreationContext, A> factory) {
    return new ReflectiveActionProvider<>(cls, messageCodec, factory, ActionOptions.defaults());
  }

  private ReflectiveActionProvider(
      Class<A> cls,
      JsonMessageCodec messageCodec,
      Function<ActionCreationContext, A> factory,
      ActionOptions options) {

    this.factory = factory;
    this.options = options.withForwardHeaders(ForwardHeadersExtractor.extractFrom(cls));
    this.messageCodec = messageCodec;

    this.componentDescriptor = ComponentDescriptor.descriptorFor(cls, messageCodec);

    this.fileDescriptor = componentDescriptor.fileDescriptor();
    this.serviceDescriptor = componentDescriptor.serviceDescriptor();
  }

  @Override
  public ActionOptions options() {
    return options;
  }

  @Override
  public Descriptors.ServiceDescriptor serviceDescriptor() {
    return serviceDescriptor;
  }

  @Override
  public ActionRouter<A> newRouter(ActionCreationContext context) {
    A action = factory.apply(context);
    return new ReflectiveActionRouter<>(action, componentDescriptor.commandHandlers(), ComponentDescriptorFactory.findIgnore(action.getClass()));
  }

  @Override
  public Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {fileDescriptor};
  }

  @Override
  public Optional<MessageCodec> alternativeCodec() {
    return Optional.of(messageCodec);
  }
}
