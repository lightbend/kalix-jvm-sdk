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

package kalix.springsdk.eventsourced;

import com.google.protobuf.Descriptors;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityOptions;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityProvider;
import kalix.javasdk.impl.MessageCodec;
import kalix.javasdk.impl.eventsourcedentity.EventSourcedEntityRouter;
import kalix.springsdk.annotations.Entity;
import kalix.springsdk.annotations.EventHandler;
import kalix.springsdk.impl.ComponentDescriptor;
import kalix.springsdk.impl.SpringSdkMessageCodec;
import kalix.springsdk.impl.eventsourcedentity.ReflectiveEventSourcedEntityRouter;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReflectiveEventSourcedEntityProvider<S, E extends EventSourcedEntity<S>>
    implements EventSourcedEntityProvider<S, E> {

  private final String entityType;
  private final Function<EventSourcedEntityContext, E> factory;
  private final EventSourcedEntityOptions options;
  private final Descriptors.FileDescriptor fileDescriptor;
  private final Descriptors.ServiceDescriptor serviceDescriptor;
  private final ComponentDescriptor componentDescriptor;

  private final Map<Class<?>, Method> eventHandlers;

  public static <S, E extends EventSourcedEntity<S>> ReflectiveEventSourcedEntityProvider<S, E> of(
      Class<E> cls, Function<EventSourcedEntityContext, E> factory) {
    return new ReflectiveEventSourcedEntityProvider<>(
        cls, factory, EventSourcedEntityOptions.defaults());
  }

  public ReflectiveEventSourcedEntityProvider(
      Class<E> entityClass,
      Function<EventSourcedEntityContext, E> factory,
      EventSourcedEntityOptions options) {

    Entity annotation = entityClass.getAnnotation(Entity.class);
    if (annotation == null)
      throw new IllegalArgumentException(
          "Event Sourced Entity [" + entityClass.getName() + "] is missing '@Entity' annotation");

    // TODO validate all methods annotated with @EventHandler only have 1 param and a correct return type

    var annotatedHandlers = Arrays.stream(entityClass.getDeclaredMethods())
            .filter(m -> m.getAnnotation(EventHandler.class) != null)
            .collect(Collectors.toList());

    var expectedReturnType = ((ParameterizedType) entityClass.getGenericSuperclass()).getActualTypeArguments()[0];
    var invalidHandlers = annotatedHandlers.stream().filter(
            m -> m.getParameterCount() != 1
                    || !Modifier.isPublic(m.getModifiers())
                    || expectedReturnType != m.getReturnType())
            .map(Method::getName)
            .collect(Collectors.toList());
    if (!invalidHandlers.isEmpty())
      throw new IllegalArgumentException(
              "Event Sourced Entity [" + entityClass.getName() + "] has '@EventHandler' methods " + invalidHandlers +
                      " with a wrong signature: it must have exactly 1 unique parameter and return type '" + expectedReturnType.getTypeName() + "'");
    // TODO extract logic
    Function<Method, Class<?>> eventTypeExtractor = (mt) -> mt.getParameterTypes()[0];
    this.eventHandlers = annotatedHandlers.stream().collect(Collectors.toMap(eventTypeExtractor, Function.identity(), (m1, m2) -> {
              throw new IllegalArgumentException("Event Sourced Entity [" + m1.getDeclaringClass().getName() + "] " +
                      "cannot have duplicate event handlers (" + m1.getName() + ", " + m2.getName() + ") for the same event type: " + m1.getParameterTypes()[0].getName() );
                    }));


    this.entityType = annotation.entityType();

    this.factory = factory;
    this.options = options;

    this.componentDescriptor = ComponentDescriptor.descriptorFor(entityClass);

    this.fileDescriptor = componentDescriptor.fileDescriptor();
    this.serviceDescriptor = componentDescriptor.serviceDescriptor();

  }

  @Override
  public EventSourcedEntityOptions options() {
    return options;
  }

  @Override
  public Descriptors.ServiceDescriptor serviceDescriptor() {
    return serviceDescriptor;
  }

  @Override
  public String entityType() {
    return entityType;
  }

  @Override
  public EventSourcedEntityRouter<S, E> newRouter(EventSourcedEntityContext context) {
    E entity = factory.apply(context);
    return new ReflectiveEventSourcedEntityRouter<>(entity, componentDescriptor.methods(), eventHandlers);
  }

  @Override
  public Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {fileDescriptor};
  }

  @Override
  public Optional<MessageCodec> alternativeCodec() {
    return Optional.of(SpringSdkMessageCodec.instance());
  }
}
