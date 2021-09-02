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
public class EventSourcedTwoEntityProvider
    implements EventSourcedEntityProvider<Persisted, EventSourcedTwoEntity> {

  private final Function<EventSourcedEntityContext, EventSourcedTwoEntity> entityFactory;
  private final EventSourcedEntityOptions options;

  /** Factory method of EventSourcedTwoEntityProvider */
  public static EventSourcedTwoEntityProvider of(
      Function<EventSourcedEntityContext, EventSourcedTwoEntity> entityFactory) {
    return new EventSourcedTwoEntityProvider(entityFactory, EventSourcedEntityOptions.defaults());
  }

  private EventSourcedTwoEntityProvider(
      Function<EventSourcedEntityContext, EventSourcedTwoEntity> entityFactory,
      EventSourcedEntityOptions options) {
    this.entityFactory = entityFactory;
    this.options = options;
  }

  @Override
  public final EventSourcedEntityOptions options() {
    return options;
  }

  public final EventSourcedTwoEntityProvider withOptions(EventSourcedEntityOptions options) {
    return new EventSourcedTwoEntityProvider(entityFactory, options);
  }

  @Override
  public final Descriptors.ServiceDescriptor serviceDescriptor() {
    return EventSourcedEntity.getDescriptor().findServiceByName("EventSourcedTwo");
  }

  @Override
  public final String entityType() {
    return "EventSourcedTwoEntity";
  }

  @Override
  public final EventSourcedTwoEntityHandler newHandler(EventSourcedEntityContext context) {
    return new EventSourcedTwoEntityHandler(entityFactory.apply(context));
  }

  @Override
  public final Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {
      EventSourcedEntity.getDescriptor(), EmptyProto.getDescriptor()
    };
  }
}
