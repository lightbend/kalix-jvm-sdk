/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kalix.springsdk.view;

import com.google.protobuf.Descriptors;
import kalix.javasdk.impl.MessageCodec;
import kalix.javasdk.impl.view.ViewRouter;
import kalix.javasdk.view.View;
import kalix.javasdk.view.ViewCreationContext;
import kalix.javasdk.view.ViewOptions;
import kalix.javasdk.view.ViewProvider;
import kalix.springsdk.annotations.ViewId;
import kalix.springsdk.impl.ComponentDescriptor;
import kalix.springsdk.impl.SpringSdkMessageCodec;
import kalix.springsdk.impl.view.ReflectiveViewRouter;

import java.util.Optional;
import java.util.function.Function;

public class ReflectiveViewProvider<S, V extends View<S>> implements ViewProvider<S, V> {
  private final Function<ViewCreationContext, V> factory;

  private final String viewId;

  private final ViewOptions options;
  private final Descriptors.FileDescriptor fileDescriptor;
  private final Descriptors.ServiceDescriptor serviceDescriptor;
  private final ComponentDescriptor componentDescriptor;

  public static <S, V extends View<S>> ReflectiveViewProvider<S, V> of(
      Class<V> cls, Function<ViewCreationContext, V> factory) {

    String viewId =
        Optional.ofNullable(cls.getAnnotation(ViewId.class))
            .map(ViewId::value)
            .orElseGet(cls::getName);

    return new ReflectiveViewProvider<>(cls, viewId, factory, ViewOptions.defaults());
  }

  private ReflectiveViewProvider(
      Class<V> cls, String viewId, Function<ViewCreationContext, V> factory, ViewOptions options) {
    this.factory = factory;
    this.options = options;
    this.viewId = viewId;

    this.componentDescriptor = ComponentDescriptor.descriptorFor(cls);

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
    return new ReflectiveViewRouter<>(view, componentDescriptor.methods());
  }

  @Override
  public Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[0];
  }

  @Override
  public Optional<MessageCodec> alternativeCodec() {
    return Optional.of(SpringSdkMessageCodec.instance());
  }
}
