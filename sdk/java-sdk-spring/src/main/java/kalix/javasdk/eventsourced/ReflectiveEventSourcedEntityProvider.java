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

package kalix.javasdk.eventsourced;

import com.google.protobuf.Descriptors;
import kalix.javasdk.common.ForwardHeadersExtractor;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityOptions;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityProvider;
import kalix.javasdk.impl.ComponentDescriptor;
import kalix.javasdk.impl.ComponentDescriptorFactory$;
import kalix.javasdk.impl.JsonMessageCodec;
import kalix.javasdk.impl.MessageCodec;
import kalix.javasdk.impl.MethodInvoker;
import kalix.javasdk.impl.eventsourcedentity.EventSourcedEntityRouter;
import kalix.javasdk.impl.eventsourcedentity.EventSourcedHandlersExtractor;
import kalix.javasdk.impl.eventsourcedentity.ReflectiveEventSourcedEntityRouter;
import scala.collection.immutable.Map;

import java.util.Optional;
import java.util.function.Function;

public class ReflectiveEventSourcedEntityProvider<S, E, ES extends EventSourcedEntity<S, E>>
    implements EventSourcedEntityProvider<S, E, ES> {

  private final String typeId;
  private final Function<EventSourcedEntityContext, ES> factory;
  private final EventSourcedEntityOptions options;
  private final Descriptors.FileDescriptor fileDescriptor;
  private final Descriptors.ServiceDescriptor serviceDescriptor;
  private final ComponentDescriptor componentDescriptor;

  private final JsonMessageCodec messageCodec;

  private final Map<String, MethodInvoker> eventHandlers;

  public static <S, E, ES extends EventSourcedEntity<S, E>> ReflectiveEventSourcedEntityProvider<S, E, ES> of(
      Class<ES> cls,
      JsonMessageCodec messageCodec,
      Function<EventSourcedEntityContext, ES> factory) {
    return new ReflectiveEventSourcedEntityProvider<>(
        cls, messageCodec, factory, EventSourcedEntityOptions.defaults());
  }

  public ReflectiveEventSourcedEntityProvider(
      Class<ES> entityClass,
      JsonMessageCodec messageCodec,
      Function<EventSourcedEntityContext, ES> factory,
      EventSourcedEntityOptions options) {

    String typeId = ComponentDescriptorFactory$.MODULE$.readTypeIdValue(entityClass);
    if (typeId == null)
      throw new IllegalArgumentException(
          "Event Sourced Entity [" + entityClass.getName() + "] is missing '@TypeId' annotation");

    this.eventHandlers = EventSourcedHandlersExtractor.handlersFrom(entityClass, messageCodec);
    this.typeId = typeId;
    this.factory = factory;
    this.options = options.withForwardHeaders(ForwardHeadersExtractor.extractFrom(entityClass));
    this.messageCodec = messageCodec;
    this.componentDescriptor = ComponentDescriptor.descriptorFor(entityClass, messageCodec);
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
  public String typeId() {
    return typeId;
  }

  @Override
  public EventSourcedEntityRouter<S, E, ES> newRouter(EventSourcedEntityContext context) {
    ES entity = factory.apply(context);
    return new ReflectiveEventSourcedEntityRouter<>(
        entity, componentDescriptor.commandHandlers(), eventHandlers, messageCodec);
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
