package org.example.eventsourcedentity;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Empty;
import com.google.protobuf.EmptyProto;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityOptions;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityProvider;
import org.example.eventsourcedentity.domain.CounterDomain;

import java.util.function.Function;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * An event sourced entity provider that defines how to register and create the entity for
 * the Protobuf service <code>CounterService</code>.
 *
 * Should be used with the <code>register</code> method in {@link kalix.javasdk.Kalix}.
 */
public class CounterServiceEntityProvider implements EventSourcedEntityProvider<CounterDomain.CounterState, Object, CounterServiceEntity> {

  private final Function<EventSourcedEntityContext, CounterServiceEntity> entityFactory;
  private final EventSourcedEntityOptions options;

  /** Factory method of CounterServiceEntityProvider */
  public static CounterServiceEntityProvider of(Function<EventSourcedEntityContext, CounterServiceEntity> entityFactory) {
    return new CounterServiceEntityProvider(entityFactory, EventSourcedEntityOptions.defaults());
  }

  private CounterServiceEntityProvider(
      Function<EventSourcedEntityContext, CounterServiceEntity> entityFactory,
      EventSourcedEntityOptions options) {
    this.entityFactory = entityFactory;
    this.options = options;
  }

  @Override
  public final EventSourcedEntityOptions options() {
    return options;
  }

  public final CounterServiceEntityProvider withOptions(EventSourcedEntityOptions options) {
    return new CounterServiceEntityProvider(entityFactory, options);
  }

  @Override
  public final Descriptors.ServiceDescriptor serviceDescriptor() {
    return CounterApi.getDescriptor().findServiceByName("CounterService");
  }

  @Override
  public final String typeId() {
    return "counter";
  }

  @Override
  public final CounterServiceEntityRouter newRouter(EventSourcedEntityContext context) {
    return new CounterServiceEntityRouter(entityFactory.apply(context));
  }

  @Override
  public final Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {
      CounterApi.getDescriptor(),
      CounterDomain.getDescriptor(),
      EmptyProto.getDescriptor()
    };
  }
}
