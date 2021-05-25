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
import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityOptions;
import com.akkaserverless.javasdk.tck.model.view.ViewTckModelBehavior;
import com.akkaserverless.javasdk.valueentity.ValueEntityOptions;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityOptions;
import com.akkaserverless.javasdk.tck.model.action.ActionTckModelBehavior;
import com.akkaserverless.javasdk.tck.model.action.ActionTwoBehavior;
import com.akkaserverless.javasdk.tck.model.replicatedentity.ConfiguredReplicatedEntity;
import com.akkaserverless.javasdk.tck.model.replicatedentity.TckModelReplicatedEntity;
import com.akkaserverless.javasdk.tck.model.replicatedentity.ReplicatedEntityTwo;
import com.akkaserverless.javasdk.tck.model.localpersistenceeventing.LocalPersistenceSubscriber;
import com.akkaserverless.javasdk.tck.model.eventsourcedentity.EventSourcedConfiguredEntity;
import com.akkaserverless.javasdk.tck.model.eventsourcedentity.EventSourcedTckModelEntity;
import com.akkaserverless.javasdk.tck.model.eventsourcedentity.EventSourcedTwoEntity;
import com.akkaserverless.javasdk.tck.model.valueentity.ValueEntityConfiguredEntity;
import com.akkaserverless.javasdk.tck.model.valueentity.ValueEntityTckModelEntity;
import com.akkaserverless.javasdk.tck.model.valueentity.ValueEntityTwoEntity;
import com.akkaserverless.tck.model.Action;
import com.akkaserverless.tck.model.eventing.LocalPersistenceEventing;
import com.akkaserverless.tck.model.EventSourcedEntity;
import com.akkaserverless.tck.model.ReplicatedEntity;
import com.akkaserverless.tck.model.ValueEntity;
import com.akkaserverless.tck.model.View;

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
          .registerValueEntity(
              ValueEntityTckModelEntity.class,
              ValueEntity.getDescriptor().findServiceByName("ValueEntityTckModel"),
              ValueEntity.getDescriptor())
          .registerValueEntity(
              ValueEntityTwoEntity.class,
              ValueEntity.getDescriptor().findServiceByName("ValueEntityTwo"))
          .registerValueEntity(
              ValueEntityConfiguredEntity.class,
              ValueEntity.getDescriptor().findServiceByName("ValueEntityConfigured"),
              ValueEntityOptions.defaults() // required timeout of 100 millis for TCK tests
                  .withPassivationStrategy(PassivationStrategy.timeout(Duration.ofMillis(100))))
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
                  .withPassivationStrategy(PassivationStrategy.timeout(Duration.ofMillis(100))))
          .registerEventSourcedEntity(
              EventSourcedTckModelEntity.class,
              EventSourcedEntity.getDescriptor().findServiceByName("EventSourcedTckModel"),
              EventSourcedEntity.getDescriptor())
          .registerEventSourcedEntity(
              EventSourcedTwoEntity.class,
              EventSourcedEntity.getDescriptor().findServiceByName("EventSourcedTwo"))
          .registerEventSourcedEntity(
              EventSourcedConfiguredEntity.class,
              EventSourcedEntity.getDescriptor().findServiceByName("EventSourcedConfigured"),
              EventSourcedEntityOptions.defaults() // required timeout of 100 millis for TCK tests
                  .withPassivationStrategy(PassivationStrategy.timeout(Duration.ofMillis(100))))
          .registerAction(
              LocalPersistenceSubscriber.class,
              LocalPersistenceEventing.getDescriptor()
                  .findServiceByName("LocalPersistenceSubscriberModel"))
          .registerEventSourcedEntity(
              com.akkaserverless.javasdk.tck.model.localpersistenceeventing.EventSourcedEntityOne
                  .class,
              LocalPersistenceEventing.getDescriptor().findServiceByName("EventSourcedEntityOne"),
              LocalPersistenceEventing.getDescriptor())
          .registerEventSourcedEntity(
              com.akkaserverless.javasdk.tck.model.localpersistenceeventing.EventSourcedEntityTwo
                  .class,
              LocalPersistenceEventing.getDescriptor().findServiceByName("EventSourcedEntityTwo"),
              LocalPersistenceEventing.getDescriptor())
          .registerValueEntity(
              com.akkaserverless.javasdk.tck.model.localpersistenceeventing.ValueEntityOne.class,
              LocalPersistenceEventing.getDescriptor().findServiceByName("ValueEntityOne"),
              LocalPersistenceEventing.getDescriptor())
          .registerValueEntity(
              com.akkaserverless.javasdk.tck.model.localpersistenceeventing.ValueEntityTwo.class,
              LocalPersistenceEventing.getDescriptor().findServiceByName("ValueEntityTwo"),
              LocalPersistenceEventing.getDescriptor())
          .registerView(
              com.akkaserverless.javasdk.tck.model.view.ViewTckModelBehavior.class,
              View.getDescriptor().findServiceByName("ViewTckModel"),
              "tck-view",
              View.getDescriptor());

  public static void main(String[] args) throws Exception {
    SERVICE.start().toCompletableFuture().get();
  }
}
