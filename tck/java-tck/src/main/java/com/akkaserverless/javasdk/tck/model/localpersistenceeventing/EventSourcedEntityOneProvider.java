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

package com.akkaserverless.javasdk.tck.model.localpersistenceeventing;

import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityContext;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityOptions;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityProvider;
import com.akkaserverless.tck.model.eventing.LocalPersistenceEventing;
import com.google.protobuf.Descriptors;
import com.google.protobuf.EmptyProto;

import java.util.function.Function;

/** An event sourced entity provider */
public class EventSourcedEntityOneProvider
    implements EventSourcedEntityProvider<String, EventSourcedEntityOne> {

  private final Function<EventSourcedEntityContext, EventSourcedEntityOne> entityFactory;
  private final EventSourcedEntityOptions options;

  /** Factory method of EventSourcedEntityOneProvider */
  public static EventSourcedEntityOneProvider of(
      Function<EventSourcedEntityContext, EventSourcedEntityOne> entityFactory) {
    return new EventSourcedEntityOneProvider(entityFactory, EventSourcedEntityOptions.defaults());
  }

  private EventSourcedEntityOneProvider(
      Function<EventSourcedEntityContext, EventSourcedEntityOne> entityFactory,
      EventSourcedEntityOptions options) {
    this.entityFactory = entityFactory;
    this.options = options;
  }

  @Override
  public final EventSourcedEntityOptions options() {
    return options;
  }

  public final EventSourcedEntityOneProvider withOptions(EventSourcedEntityOptions options) {
    return new EventSourcedEntityOneProvider(entityFactory, options);
  }

  @Override
  public final Descriptors.ServiceDescriptor serviceDescriptor() {
    return LocalPersistenceEventing.getDescriptor().findServiceByName("EventSourcedEntityOne");
  }

  @Override
  public final String entityType() {
    return "eventlogeventing-one";
  }

  @Override
  public final EventSourcedEntityOneHandler newHandler(EventSourcedEntityContext context) {
    return new EventSourcedEntityOneHandler(entityFactory.apply(context));
  }

  @Override
  public final Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {
      LocalPersistenceEventing.getDescriptor(), EmptyProto.getDescriptor()
    };
  }
}
