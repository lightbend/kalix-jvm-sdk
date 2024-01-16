/*
 * Copyright 2024 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
