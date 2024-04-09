/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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
public class EventSourcedEntityOneProvider
    implements EventSourcedEntityProvider<String, Object, EventSourcedEntityOne> {

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
  public final String typeId() {
    return "eventlogeventing-one";
  }

  @Override
  public final EventSourcedEntityOneRouter newRouter(EventSourcedEntityContext context) {
    return new EventSourcedEntityOneRouter(entityFactory.apply(context));
  }

  @Override
  public final Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {
      LocalPersistenceEventing.getDescriptor(), EmptyProto.getDescriptor()
    };
  }
}
