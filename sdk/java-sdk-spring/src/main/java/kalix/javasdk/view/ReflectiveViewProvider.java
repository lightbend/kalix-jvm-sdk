/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.view;

import com.google.protobuf.Descriptors;
import kalix.javasdk.annotations.ViewId;
import kalix.javasdk.impl.MessageCodec;
import kalix.javasdk.impl.view.ViewRouter;
import kalix.javasdk.impl.ComponentDescriptor;
import kalix.javasdk.impl.ComponentDescriptorFactory;
import kalix.javasdk.impl.JsonMessageCodec;
import kalix.javasdk.impl.view.ReflectiveViewRouter;

import java.util.Optional;
import java.util.function.Function;

public class ReflectiveViewProvider<S, V extends View<S>> implements ViewProvider {
  private final Function<ViewCreationContext, V> factory;

  private final String viewId;

  private final ViewOptions options;
  private final Descriptors.FileDescriptor fileDescriptor;
  private final Descriptors.ServiceDescriptor serviceDescriptor;
  private final ComponentDescriptor componentDescriptor;

  private final JsonMessageCodec messageCodec;

  public static <S, V extends View<S>> ReflectiveViewProvider<S, V> of(
      Class<V> cls, JsonMessageCodec messageCodec, Function<ViewCreationContext, V> factory) {

    String viewId =
        Optional.ofNullable(cls.getAnnotation(ViewId.class))
            .map(ViewId::value)
            .orElseGet(cls::getName);

    return new ReflectiveViewProvider<>(cls, messageCodec, viewId, factory, ViewOptions.defaults());
  }

  private ReflectiveViewProvider(
      Class<V> cls,
      JsonMessageCodec messageCodec,
      String viewId,
      Function<ViewCreationContext, V> factory,
      ViewOptions options) {
    this.factory = factory;
    this.options = options;
    this.messageCodec = messageCodec;
    this.viewId = viewId;

    this.componentDescriptor = ComponentDescriptor.descriptorFor(cls, messageCodec);

    this.fileDescriptor = componentDescriptor.fileDescriptor();
    this.serviceDescriptor = componentDescriptor.serviceDescriptor();
  }

  @Override
  public Descriptors.ServiceDescriptor serviceDescriptor() {
    return serviceDescriptor;
  }

  @Override
  public String viewId() {
    return viewId;
  }

  @Override
  public ViewOptions options() {
    return options;
  }

  @Override
  public ViewRouter<S, V> newRouter(ViewCreationContext context) {
    V view = factory.apply(context);
    return new ReflectiveViewRouter<>(view, componentDescriptor.commandHandlers(), ComponentDescriptorFactory.findIgnore(view.getClass()));
  }

  @Override
  public Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[0];
  }

  @Override
  public Optional<MessageCodec> alternativeCodec() {
    return Optional.of(messageCodec);
  }
}
