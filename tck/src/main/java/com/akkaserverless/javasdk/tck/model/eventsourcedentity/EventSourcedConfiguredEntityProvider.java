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

package com.akkaserverless.javasdk.tck.model.eventsourcedentity;

import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityContext;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityOptions;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityProvider;
import com.akkaserverless.tck.model.EventSourcedEntity;
import com.akkaserverless.tck.model.EventSourcedEntity.Persisted;
import com.google.protobuf.Descriptors;
import com.google.protobuf.EmptyProto;

import java.util.function.Function;

/** An event sourced entity provider */
public class EventSourcedConfiguredEntityProvider
    implements EventSourcedEntityProvider<Persisted, EventSourcedConfiguredEntity> {

  private final Function<EventSourcedEntityContext, EventSourcedConfiguredEntity> entityFactory;
  private final EventSourcedEntityOptions options;

  /** Factory method of EventSourcedConfiguredEntityProvider */
  public static EventSourcedConfiguredEntityProvider of(
      Function<EventSourcedEntityContext, EventSourcedConfiguredEntity> entityFactory) {
    return new EventSourcedConfiguredEntityProvider(
        entityFactory, EventSourcedEntityOptions.defaults());
  }

  private EventSourcedConfiguredEntityProvider(
      Function<EventSourcedEntityContext, EventSourcedConfiguredEntity> entityFactory,
      EventSourcedEntityOptions options) {
    this.entityFactory = entityFactory;
    this.options = options;
  }

  @Override
  public final EventSourcedEntityOptions options() {
    return options;
  }

  public final EventSourcedConfiguredEntityProvider withOptions(EventSourcedEntityOptions options) {
    return new EventSourcedConfiguredEntityProvider(entityFactory, options);
  }

  @Override
  public final Descriptors.ServiceDescriptor serviceDescriptor() {
    return EventSourcedEntity.getDescriptor().findServiceByName("EventSourcedConfigured");
  }

  @Override
  public final String entityType() {
    return "event-sourced-configured";
  }

  @Override
  public final EventSourcedConfiguredEntityHandler newHandler(EventSourcedEntityContext context) {
    return new EventSourcedConfiguredEntityHandler(entityFactory.apply(context));
  }

  @Override
  public final Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {
      EventSourcedEntity.getDescriptor(), EmptyProto.getDescriptor()
    };
  }
}
