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

package kalix.javasdk.tck.model.localpersistenceeventing;

import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityOptions;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityProvider;
import kalix.tck.model.eventing.LocalPersistenceEventing;
import com.google.protobuf.Descriptors;
import com.google.protobuf.EmptyProto;

import java.util.function.Function;

/** An event sourced entity provider */
public class EventSourcedEntityTwoProvider
    implements EventSourcedEntityProvider<String, Object, EventSourcedEntityTwo> {

  private final Function<EventSourcedEntityContext, EventSourcedEntityTwo> entityFactory;
  private final EventSourcedEntityOptions options;

  /** Factory method of EventSourcedEntityTwoProvider */
  public static EventSourcedEntityTwoProvider of(
      Function<EventSourcedEntityContext, EventSourcedEntityTwo> entityFactory) {
    return new EventSourcedEntityTwoProvider(entityFactory, EventSourcedEntityOptions.defaults());
  }

  private EventSourcedEntityTwoProvider(
      Function<EventSourcedEntityContext, EventSourcedEntityTwo> entityFactory,
      EventSourcedEntityOptions options) {
    this.entityFactory = entityFactory;
    this.options = options;
  }

  @Override
  public final EventSourcedEntityOptions options() {
    return options;
  }

  public final EventSourcedEntityTwoProvider withOptions(EventSourcedEntityOptions options) {
    return new EventSourcedEntityTwoProvider(entityFactory, options);
  }

  @Override
  public final Descriptors.ServiceDescriptor serviceDescriptor() {
    return LocalPersistenceEventing.getDescriptor().findServiceByName("EventSourcedEntityTwo");
  }

  @Override
  public final String typeId() {
    return "eventlogeventing-two";
  }

  @Override
  public final EventSourcedEntityTwoRouter newRouter(EventSourcedEntityContext context) {
    return new EventSourcedEntityTwoRouter(entityFactory.apply(context));
  }

  @Override
  public final Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {
      LocalPersistenceEventing.getDescriptor(), EmptyProto.getDescriptor()
    };
  }
}
