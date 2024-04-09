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
