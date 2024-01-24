package org.example.domain;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Empty;
import com.google.protobuf.EmptyProto;
import kalix.javasdk.valueentity.ValueEntityContext;
import kalix.javasdk.valueentity.ValueEntityOptions;
import kalix.javasdk.valueentity.ValueEntityProvider;
import org.example.state.OuterCounterState;
import org.example.valueentity.CounterApi;

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
public class CounterProvider implements ValueEntityProvider<OuterCounterState.CounterState, Counter> {

  private final Function<ValueEntityContext, Counter> entityFactory;
  private final ValueEntityOptions options;

  /** Factory method of CounterProvider */
  public static CounterProvider of(Function<ValueEntityContext, Counter> entityFactory) {
    return new CounterProvider(entityFactory, ValueEntityOptions.defaults());
  }

  private CounterProvider(
      Function<ValueEntityContext, Counter> entityFactory,
      ValueEntityOptions options) {
    this.entityFactory = entityFactory;
    this.options = options;
  }

  @Override
  public final ValueEntityOptions options() {
    return options;
  }

  public final CounterProvider withOptions(ValueEntityOptions options) {
    return new CounterProvider(entityFactory, options);
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
  public final CounterRouter newRouter(ValueEntityContext context) {
    return new CounterRouter(entityFactory.apply(context));
  }

  @Override
  public final Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {
      CounterApi.getDescriptor(),
      EmptyProto.getDescriptor(),
      OuterCounterState.getDescriptor()
    };
  }
}
