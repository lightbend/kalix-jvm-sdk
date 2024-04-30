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
public class ValueEntityTwoProvider implements ValueEntityProvider<Object, ValueEntityTwo> {

  private final Function<ValueEntityContext, ValueEntityTwo> entityFactory;
  private final ValueEntityOptions options;

  /** Factory method of ShoppingCartProvider */
  public static ValueEntityTwoProvider of(
      Function<ValueEntityContext, ValueEntityTwo> entityFactory) {
    return new ValueEntityTwoProvider(entityFactory, ValueEntityOptions.defaults());
  }

  private ValueEntityTwoProvider(
      Function<ValueEntityContext, ValueEntityTwo> entityFactory, ValueEntityOptions options) {
    this.entityFactory = entityFactory;
    this.options = options;
  }

  @Override
  public final ValueEntityOptions options() {
    return options;
  }

  public final ValueEntityTwoProvider withOptions(ValueEntityOptions options) {
    return new ValueEntityTwoProvider(entityFactory, options);
  }

  @Override
  public final Descriptors.ServiceDescriptor serviceDescriptor() {
    return LocalPersistenceEventing.getDescriptor().findServiceByName("ValueEntityTwo");
  }

  @Override
  public final String typeId() {
    return "valuechangeseventing-two";
  }

  @Override
  public final ValueEntityTwoRouter newRouter(ValueEntityContext context) {
    return new ValueEntityTwoRouter(entityFactory.apply(context));
  }

  @Override
  public final Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {
      LocalPersistenceEventing.getDescriptor(), EmptyProto.getDescriptor()
    };
  }
}
