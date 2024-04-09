/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.tck.model.replicatedentity;

import kalix.replicatedentity.ReplicatedData;
import kalix.javasdk.replicatedentity.ReplicatedEntityContext;
import kalix.javasdk.replicatedentity.ReplicatedEntityOptions;
import kalix.javasdk.replicatedentity.ReplicatedEntityProvider;
import kalix.tck.model.ReplicatedEntity;
import com.google.protobuf.Descriptors;
import com.google.protobuf.EmptyProto;

import java.util.function.Function;

public class ReplicatedEntityTckModelEntityProvider
    implements ReplicatedEntityProvider<ReplicatedData, ReplicatedEntityTckModelEntity> {

  private final Function<ReplicatedEntityContext, ReplicatedEntityTckModelEntity> entityFactory;
  private final ReplicatedEntityOptions options;

  public static ReplicatedEntityTckModelEntityProvider of(
      Function<ReplicatedEntityContext, ReplicatedEntityTckModelEntity> entityFactory) {
    return new ReplicatedEntityTckModelEntityProvider(
        entityFactory, ReplicatedEntityOptions.defaults());
  }

  private ReplicatedEntityTckModelEntityProvider(
      Function<ReplicatedEntityContext, ReplicatedEntityTckModelEntity> entityFactory,
      ReplicatedEntityOptions options) {
    this.entityFactory = entityFactory;
    this.options = options;
  }

  @Override
  public final ReplicatedEntityOptions options() {
    return options;
  }

  public final ReplicatedEntityTckModelEntityProvider withOptions(ReplicatedEntityOptions options) {
    return new ReplicatedEntityTckModelEntityProvider(entityFactory, options);
  }

  @Override
  public final Descriptors.ServiceDescriptor serviceDescriptor() {
    return ReplicatedEntity.getDescriptor().findServiceByName("ReplicatedEntityTckModel");
  }

  @Override
  public final String typeId() {
    return "replicated-entity-tck-model";
  }

  @Override
  public final ReplicatedEntityTckModelEntityRouter newRouter(ReplicatedEntityContext context) {
    return new ReplicatedEntityTckModelEntityRouter(entityFactory.apply(context));
  }

  @Override
  public final Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {
      ReplicatedEntity.getDescriptor(), EmptyProto.getDescriptor()
    };
  }
}
