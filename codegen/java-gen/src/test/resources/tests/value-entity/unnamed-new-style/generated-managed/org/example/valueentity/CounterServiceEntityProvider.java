package org.example.valueentity;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Empty;
import com.google.protobuf.EmptyProto;
import kalix.javasdk.valueentity.ValueEntityContext;
import kalix.javasdk.valueentity.ValueEntityOptions;
import kalix.javasdk.valueentity.ValueEntityProvider;
import org.example.valueentity.domain.CounterDomain;

import java.util.function.Function;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * A value entity provider that defines how to register and create the entity for
 * the Protobuf service <code>CounterService</code>.
 *
 * Should be used with the <code>register</code> method in {@link kalix.javasdk.Kalix}.
 */
public class CounterServiceEntityProvider implements ValueEntityProvider<CounterDomain.CounterState, CounterServiceEntity> {

  private final Function<ValueEntityContext, CounterServiceEntity> entityFactory;
  private final ValueEntityOptions options;

  /** Factory method of CounterServiceEntityProvider */
  public static CounterServiceEntityProvider of(Function<ValueEntityContext, CounterServiceEntity> entityFactory) {
    return new CounterServiceEntityProvider(entityFactory, ValueEntityOptions.defaults());
  }

  private CounterServiceEntityProvider(
      Function<ValueEntityContext, CounterServiceEntity> entityFactory,
      ValueEntityOptions options) {
    this.entityFactory = entityFactory;
    this.options = options;
  }

  @Override
  public final ValueEntityOptions options() {
    return options;
  }

  public final CounterServiceEntityProvider withOptions(ValueEntityOptions options) {
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
  public final CounterServiceEntityRouter newRouter(ValueEntityContext context) {
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
