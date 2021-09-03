// FIXME codegen for ReplicatedEntity

package com.example.counter.domain;

import com.akkaserverless.javasdk.replicatedentity.ReplicatedCounter;
import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityOptions;
import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityProvider;
import com.example.counter.CounterApi;
import com.example.counter.domain.CounterDomain;
import com.google.protobuf.Descriptors;
import com.google.protobuf.EmptyProto;
import java.util.function.Function;

public class CounterProvider implements ReplicatedEntityProvider<ReplicatedCounter, Counter> {

  private final Function<ReplicatedEntityContext, Counter> entityFactory;
  private final ReplicatedEntityOptions options;

  public static CounterProvider of(Function<ReplicatedEntityContext, Counter> entityFactory) {
    return new CounterProvider(entityFactory, ReplicatedEntityOptions.defaults());
  }

  private CounterProvider(
      Function<ReplicatedEntityContext, Counter> entityFactory,
      ReplicatedEntityOptions options) {
    this.entityFactory = entityFactory;
    this.options = options;
  }

  @Override
  public final ReplicatedEntityOptions options() {
    return options;
  }

  public final CounterProvider withOptions(ReplicatedEntityOptions options) {
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
  public final CounterHandler newHandler(ReplicatedEntityContext context) {
    return new CounterHandler(entityFactory.apply(context));
  }

  @Override
  public final Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {
      EmptyProto.getDescriptor(),
      CounterApi.getDescriptor(),
      CounterDomain.getDescriptor()
    };
  }
}
