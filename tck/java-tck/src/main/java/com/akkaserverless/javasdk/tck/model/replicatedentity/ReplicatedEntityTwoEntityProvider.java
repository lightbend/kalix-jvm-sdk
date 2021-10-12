/*
 * Copyright 2021 Lightbend Inc.
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

package com.akkaserverless.javasdk.tck.model.replicatedentity;

import com.akkaserverless.javasdk.replicatedentity.ReplicatedCounter;
import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityContext;
import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityOptions;
import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityProvider;
import com.akkaserverless.tck.model.ReplicatedEntity;
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

  @Override
  public final String entityType() {
    return "replicated-entity-tck-model-two";
  }

  @Override
  public final ReplicatedEntityTwoEntityHandler newHandler(ReplicatedEntityContext context) {
    return new ReplicatedEntityTwoEntityHandler(entityFactory.apply(context));
  }

  @Override
  public final Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {
      ReplicatedEntity.getDescriptor(), EmptyProto.getDescriptor()
    };
  }
}
