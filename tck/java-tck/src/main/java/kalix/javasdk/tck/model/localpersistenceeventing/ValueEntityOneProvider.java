/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.tck.model.localpersistenceeventing;

import kalix.javasdk.valueentity.ValueEntityContext;
import kalix.javasdk.valueentity.ValueEntityOptions;
import kalix.javasdk.valueentity.ValueEntityProvider;
import kalix.tck.model.eventing.LocalPersistenceEventing;
import com.google.protobuf.Descriptors;
import com.google.protobuf.EmptyProto;

import java.util.function.Function;

/** A value entity provider */
public class ValueEntityOneProvider implements ValueEntityProvider<Object, ValueEntityOne> {

  private final Function<ValueEntityContext, ValueEntityOne> entityFactory;
  private final ValueEntityOptions options;

  /** Factory method of ShoppingCartProvider */
  public static ValueEntityOneProvider of(
      Function<ValueEntityContext, ValueEntityOne> entityFactory) {
    return new ValueEntityOneProvider(entityFactory, ValueEntityOptions.defaults());
  }

  private ValueEntityOneProvider(
      Function<ValueEntityContext, ValueEntityOne> entityFactory, ValueEntityOptions options) {
    this.entityFactory = entityFactory;
    this.options = options;
  }

  @Override
  public final ValueEntityOptions options() {
    return options;
  }

  public final ValueEntityOneProvider withOptions(ValueEntityOptions options) {
    return new ValueEntityOneProvider(entityFactory, options);
  }

  @Override
  public final Descriptors.ServiceDescriptor serviceDescriptor() {
    return LocalPersistenceEventing.getDescriptor().findServiceByName("ValueEntityOne");
  }

  @Override
  public final String typeId() {
    return "valuechangeseventing-one";
  }

  @Override
  public final ValueEntityOneRouter newRouter(ValueEntityContext context) {
    return new ValueEntityOneRouter(entityFactory.apply(context));
  }

  @Override
  public final Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {
      LocalPersistenceEventing.getDescriptor(), EmptyProto.getDescriptor()
    };
  }
}
