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
