/*
 * Copyright 2024 Lightbend Inc.
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

package kalix.javasdk.view;

import com.google.protobuf.Descriptors;
import kalix.javasdk.annotations.ViewId;
import kalix.javasdk.impl.MessageCodec;
import kalix.javasdk.impl.view.ViewMultiTableRouter;
import kalix.javasdk.impl.ComponentDescriptor;
import kalix.spring.impl.KalixSpringApplication;
import kalix.javasdk.impl.JsonMessageCodec;
import kalix.javasdk.impl.view.ReflectiveViewMultiTableRouter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

public class ReflectiveMultiTableViewProvider<V> implements ViewProvider {

  private final Class<V> viewClass;
  private final BiFunction<Class<View<?>>, ViewCreationContext, View<?>> factory;
  private final String viewId;
  private final ViewOptions options;
  private final JsonMessageCodec messageCodec;
  private final ComponentDescriptor componentDescriptor;

  public static <V> ReflectiveMultiTableViewProvider<V> of(
      Class<V> viewClass,
      JsonMessageCodec messageCodec,
      BiFunction<Class<View<?>>, ViewCreationContext, View<?>> factory) {

    String viewId =
        Optional.ofNullable(viewClass.getAnnotation(ViewId.class))
            .map(ViewId::value)
            .orElseGet(viewClass::getName);

    return new ReflectiveMultiTableViewProvider<>(
        viewClass, factory, viewId, ViewOptions.defaults(), messageCodec);
  }

  private ReflectiveMultiTableViewProvider(
      Class<V> viewClass,
      BiFunction<Class<View<?>>, ViewCreationContext, View<?>> factory,
      String viewId,
      ViewOptions options,
      JsonMessageCodec messageCodec) {
    this.viewClass = viewClass;
    this.factory = factory;
    this.viewId = viewId;
    this.options = options;
    this.messageCodec = messageCodec;
    this.componentDescriptor = ComponentDescriptor.descriptorFor(viewClass, messageCodec);
  }

  @Override
  public Descriptors.ServiceDescriptor serviceDescriptor() {
    return componentDescriptor.serviceDescriptor();
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
  public ViewMultiTableRouter newRouter(ViewCreationContext context) {
    Map<Class<View<?>>, View<?>> viewTables = new HashMap<>();
    for (Class<?> innerClass : viewClass.getDeclaredClasses()) {
      if (KalixSpringApplication.isNestedViewTable(innerClass)) {
        @SuppressWarnings("unchecked")
        Class<View<?>> viewTableClass = (Class<View<?>>) innerClass;
        viewTables.put(viewTableClass, factory.apply(viewTableClass, context));
      }
    }
    return new ReflectiveViewMultiTableRouter(viewTables, componentDescriptor.commandHandlers());
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
