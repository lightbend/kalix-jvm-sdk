/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.tck.model.replicatedentity;

import kalix.javasdk.replicatedentity.ReplicatedCounter;
import kalix.javasdk.replicatedentity.ReplicatedEntityContext;
import kalix.javasdk.replicatedentity.ReplicatedEntityOptions;
import kalix.javasdk.replicatedentity.ReplicatedEntityProvider;
import kalix.tck.model.ReplicatedEntity;
import com.google.protobuf.Descriptors;
import com.google.protobuf.EmptyProto;

import java.util.function.Function;

public class ReplicatedEntityTwoEntityProvider
    implements ReplicatedEntityProvider<ReplicatedCounter, ReplicatedEntityTwoEntity> {

  private final Function<ReplicatedEntityContext, ReplicatedEntityTwoEntity> entityFactory;
  private final ReplicatedEntityOptions options;

  public static ReplicatedEntityTwoEntityProvider of(
      Function<ReplicatedEntityContext, ReplicatedEntityTwoEntity> entityFactory) {
    return new ReplicatedEntityTwoEntityProvider(entityFactory, ReplicatedEntityOptions.defaults());
  }

  private ReplicatedEntityTwoEntityProvider(
      Function<ReplicatedEntityContext, ReplicatedEntityTwoEntity> entityFactory,
      ReplicatedEntityOptions options) {
    this.entityFactory = entityFactory;
    this.options = options;
  }

  @Override
  public final ReplicatedEntityOptions options() {
    return options;
  }

  public final ReplicatedEntityTwoEntityProvider withOptions(ReplicatedEntityOptions options) {
    return new ReplicatedEntityTwoEntityProvider(entityFactory, options);
  }

  @Override
  public final Descriptors.ServiceDescriptor serviceDescriptor() {
    return ReplicatedEntity.getDescriptor().findServiceByName("ReplicatedEntityTwo");
  }

  public final String typeId() {
    return "replicated-entity-tck-model-two";
  }

  @Override
  public final ReplicatedEntityTwoEntityRouter newRouter(ReplicatedEntityContext context) {
    return new ReplicatedEntityTwoEntityRouter(entityFactory.apply(context));
  }

  @Override
  public final Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {
      ReplicatedEntity.getDescriptor(), EmptyProto.getDescriptor()
    };
  }
}
