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

import kalix.javasdk.replicatedentity.ReplicatedCounter;
import kalix.javasdk.replicatedentity.ReplicatedEntityContext;
import kalix.javasdk.replicatedentity.ReplicatedEntityOptions;
import kalix.javasdk.replicatedentity.ReplicatedEntityProvider;
import kalix.tck.model.ReplicatedEntity;
import com.google.protobuf.Descriptors;
import com.google.protobuf.EmptyProto;

import java.util.function.Function;

public class ReplicatedEntityConfiguredEntityProvider
    implements ReplicatedEntityProvider<ReplicatedCounter, ReplicatedEntityConfiguredEntity> {

  private final Function<ReplicatedEntityContext, ReplicatedEntityConfiguredEntity> entityFactory;
  private final ReplicatedEntityOptions options;

  public static ReplicatedEntityConfiguredEntityProvider of(
      Function<ReplicatedEntityContext, ReplicatedEntityConfiguredEntity> entityFactory) {
    return new ReplicatedEntityConfiguredEntityProvider(
        entityFactory, ReplicatedEntityOptions.defaults());
  }

  private ReplicatedEntityConfiguredEntityProvider(
      Function<ReplicatedEntityContext, ReplicatedEntityConfiguredEntity> entityFactory,
      ReplicatedEntityOptions options) {
    this.entityFactory = entityFactory;
    this.options = options;
  }

  @Override
  public final ReplicatedEntityOptions options() {
    return options;
  }

  public final ReplicatedEntityConfiguredEntityProvider withOptions(
      ReplicatedEntityOptions options) {
    return new ReplicatedEntityConfiguredEntityProvider(entityFactory, options);
  }

  @Override
  public final Descriptors.ServiceDescriptor serviceDescriptor() {
    return ReplicatedEntity.getDescriptor().findServiceByName("ReplicatedEntityConfigured");
  }

  @Override
  public final String typeId() {
    return "replicated-entity-configured";
  }

  @Override
  public final ReplicatedEntityConfiguredEntityRouter newRouter(ReplicatedEntityContext context) {
    return new ReplicatedEntityConfiguredEntityRouter(entityFactory.apply(context));
  }

  @Override
  public final Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {
      ReplicatedEntity.getDescriptor(), EmptyProto.getDescriptor()
    };
  }
}
