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

package com.akkaserverless.javasdk.tck;

import com.akkaserverless.javasdk.AkkaServerless;
import com.akkaserverless.javasdk.PassivationStrategy;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityOptions;
import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityOptions;
import com.akkaserverless.javasdk.replicatedentity.WriteConsistency;
import com.akkaserverless.javasdk.tck.model.action.ActionTckModelBehavior;
import com.akkaserverless.javasdk.tck.model.action.ActionTwoBehavior;
import com.akkaserverless.javasdk.tck.model.localpersistenceeventing.LocalPersistenceSubscriber;
import com.akkaserverless.javasdk.tck.model.replicatedentity.ConfiguredReplicatedEntity;
import com.akkaserverless.javasdk.tck.model.replicatedentity.ReplicatedEntityTwo;
import com.akkaserverless.javasdk.tck.model.replicatedentity.TckModelReplicatedEntity;
import com.akkaserverless.javasdk.tck.model.valueentity.ValueEntityConfiguredEntity;
import com.akkaserverless.javasdk.tck.model.valueentity.ValueEntityConfiguredEntityProvider;
import com.akkaserverless.javasdk.tck.model.valueentity.ValueEntityTckModelEntity;
import com.akkaserverless.javasdk.tck.model.valueentity.ValueEntityTckModelEntityProvider;
import com.akkaserverless.javasdk.tck.model.valueentity.ValueEntityTwoEntity;
import com.akkaserverless.javasdk.tck.model.valueentity.ValueEntityTwoEntityProvider;
import com.akkaserverless.javasdk.tck.model.view.ViewTckSourceEntity;
import com.akkaserverless.javasdk.tck.model.view.ViewTckSourceEntityProvider;
import com.akkaserverless.javasdk.valueentity.ValueEntityOptions;
import com.akkaserverless.tck.model.Action;
import com.akkaserverless.tck.model.ReplicatedEntity;
import com.akkaserverless.tck.model.View;
import com.akkaserverless.tck.model.eventing.LocalPersistenceEventing;

import java.time.Duration;

public final class JavaSdkTck {
  public static AkkaServerless SERVICE =
      new AkkaServerless()
          .registerAction(
              ActionTckModelBehavior.class,
              Action.getDescriptor().findServiceByName("ActionTckModel"),
              Action.getDescriptor())
          .registerAction(
              ActionTwoBehavior.class,
              Action.getDescriptor().findServiceByName("ActionTwo"),
              Action.getDescriptor())
          .register(ValueEntityTckModelEntityProvider.of(ValueEntityTckModelEntity::new))
          .register(ValueEntityTwoEntityProvider.of(ValueEntityTwoEntity::new))
          .register(
              ValueEntityConfiguredEntityProvider.of(ValueEntityConfiguredEntity::new)
                  .withOptions(
                      ValueEntityOptions.defaults() // required timeout of 100 millis for TCK tests
                          .withPassivationStrategy(
                              PassivationStrategy.timeout(Duration.ofMillis(100)))))
          .registerReplicatedEntity(
              TckModelReplicatedEntity.class,
              ReplicatedEntity.getDescriptor().findServiceByName("ReplicatedEntityTckModel"),
              ReplicatedEntity.getDescriptor())
          .registerReplicatedEntity(
              ReplicatedEntityTwo.class,
              ReplicatedEntity.getDescriptor().findServiceByName("ReplicatedEntityTwo"))
          .registerReplicatedEntity(
              ConfiguredReplicatedEntity.class,
              ReplicatedEntity.getDescriptor().findServiceByName("ReplicatedEntityConfigured"),
              ReplicatedEntityOptions.defaults() // required timeout of 100 millis for TCK tests
                  .withPassivationStrategy(PassivationStrategy.timeout(Duration.ofMillis(100)))
                  .withWriteConsistency(WriteConsistency.ALL))
          .register(
              com.akkaserverless.javasdk.tck.model.eventsourcedentity
                  .EventSourcedTckModelEntityProvider.of(
                      com.akkaserverless.javasdk.tck.model.eventsourcedentity
                              .EventSourcedTckModelEntity
                          ::new)
                  .withOptions(EventSourcedEntityOptions.defaults().withSnapshotEvery(5)))
          .register(
              com.akkaserverless.javasdk.tck.model.eventsourcedentity.EventSourcedTwoEntityProvider
                  .of(
                      com.akkaserverless.javasdk.tck.model.eventsourcedentity.EventSourcedTwoEntity
                          ::new))
          .register(
              com.akkaserverless.javasdk.tck.model.eventsourcedentity
                  .EventSourcedConfiguredEntityProvider.of(
                      com.akkaserverless.javasdk.tck.model.eventsourcedentity
                              .EventSourcedConfiguredEntity
                          ::new)
                  // required timeout of 100 millis for TCK tests
                  .withOptions(
                      EventSourcedEntityOptions.defaults()
                          .withPassivationStrategy(
                              PassivationStrategy.timeout(Duration.ofMillis(100)))))
          .registerAction(
              LocalPersistenceSubscriber.class,
              LocalPersistenceEventing.getDescriptor()
                  .findServiceByName("LocalPersistenceSubscriberModel"))
          .register(
              com.akkaserverless.javasdk.tck.model.localpersistenceeventing
                  .EventSourcedEntityOneProvider.of(
                  com.akkaserverless.javasdk.tck.model.localpersistenceeventing
                          .EventSourcedEntityOne
                      ::new))
          .register(
              com.akkaserverless.javasdk.tck.model.localpersistenceeventing
                  .EventSourcedEntityTwoProvider.of(
                  com.akkaserverless.javasdk.tck.model.localpersistenceeventing
                          .EventSourcedEntityTwo
                      ::new))
          .register(
              com.akkaserverless.javasdk.tck.model.localpersistenceeventing.ValueEntityOneProvider
                  .of(
                      com.akkaserverless.javasdk.tck.model.localpersistenceeventing.ValueEntityOne
                          ::new))
          .register(
              com.akkaserverless.javasdk.tck.model.localpersistenceeventing.ValueEntityTwoProvider
                  .of(
                      com.akkaserverless.javasdk.tck.model.localpersistenceeventing.ValueEntityTwo
                          ::new))
          .registerView(
              com.akkaserverless.javasdk.tck.model.view.ViewTckModelBehavior.class,
              View.getDescriptor().findServiceByName("ViewTckModel"),
              "tck-view",
              View.getDescriptor())
          .register(ViewTckSourceEntityProvider.of(ViewTckSourceEntity::new));

  public static void main(String[] args) throws Exception {
    SERVICE.start().toCompletableFuture().get();
  }
}
