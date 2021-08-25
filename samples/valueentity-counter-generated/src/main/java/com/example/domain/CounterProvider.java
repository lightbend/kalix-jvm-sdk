/* This code is managed by Akka Serverless tooling.
 * It will be re-generated to reflect any changes to your protobuf definitions.
 * DO NOT EDIT
 */
package com.example.domain;

import com.akkaserverless.javasdk.valueentity.ValueEntityContext;
import com.akkaserverless.javasdk.valueentity.ValueEntityOptions;
import com.akkaserverless.javasdk.valueentity.ValueEntityProvider;
import com.example.CounterApi;
import com.example.domain.CounterDomain;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Empty;
import com.google.protobuf.EmptyProto;
import java.util.function.Function;

/** A value entity provider */
public class CounterProvider implements ValueEntityProvider {

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
  public final String entityType() {
    return "counter";
  }

  @Override
  public final CounterHandler newHandler(ValueEntityContext context) {
    return new CounterHandler(entityFactory.apply(context));
  }

  @Override
  public final Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {
      CounterDomain.getDescriptor(),
      EmptyProto.getDescriptor()
    };
  }
}