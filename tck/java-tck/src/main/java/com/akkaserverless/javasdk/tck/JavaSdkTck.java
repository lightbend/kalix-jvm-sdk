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
import com.akkaserverless.javasdk.tck.model.localpersistenceeventing.EventSourcedEntityOne;
import com.akkaserverless.javasdk.tck.model.localpersistenceeventing.EventSourcedEntityOneProvider;
import com.akkaserverless.javasdk.tck.model.localpersistenceeventing.EventSourcedEntityTwo;
import com.akkaserverless.javasdk.tck.model.localpersistenceeventing.EventSourcedEntityTwoProvider;
import com.akkaserverless.javasdk.tck.model.localpersistenceeventing.LocalPersistenceSubscriber;
import com.akkaserverless.javasdk.tck.model.localpersistenceeventing.LocalPersistenceSubscriberProvider;
import com.akkaserverless.javasdk.tck.model.localpersistenceeventing.ValueEntityOne;
import com.akkaserverless.javasdk.tck.model.localpersistenceeventing.ValueEntityOneProvider;
import com.akkaserverless.javasdk.tck.model.localpersistenceeventing.ValueEntityTwo;
import com.akkaserverless.javasdk.tck.model.localpersistenceeventing.ValueEntityTwoProvider;
import com.akkaserverless.javasdk.tck.model.replicatedentity.ReplicatedEntityConfiguredEntity;
import com.akkaserverless.javasdk.tck.model.replicatedentity.ReplicatedEntityConfiguredEntityProvider;
import com.akkaserverless.javasdk.tck.model.replicatedentity.ReplicatedEntityTckModelEntity;
import com.akkaserverless.javasdk.tck.model.replicatedentity.ReplicatedEntityTckModelEntityProvider;
import com.akkaserverless.javasdk.tck.model.replicatedentity.ReplicatedEntityTwoEntity;
import com.akkaserverless.javasdk.tck.model.replicatedentity.ReplicatedEntityTwoEntityProvider;

import com.akkaserverless.javasdk.tck.model.view.ViewTckModelBehavior;
import com.akkaserverless.javasdk.tck.model.view.ViewTckModelBehaviorProvider;
import com.akkaserverless.javasdk.tck.model.view.ViewTckSourceEntity;
import com.akkaserverless.javasdk.tck.model.view.ViewTckSourceEntityProvider;
import com.akkaserverless.javasdk.valueentity.ValueEntityOptions;
import com.akkaserverless.tck.model.action.ActionTckModelActionProvider;
import com.akkaserverless.tck.model.action.ActionTwoActionProvider;
import com.akkaserverless.tck.model.action.ActionTckModelImpl;
import com.akkaserverless.tck.model.action.ActionTwoImpl;
import com.akkaserverless.tck.model.eventsourcedentity.EventSourcedConfiguredEntity;
import com.akkaserverless.tck.model.eventsourcedentity.EventSourcedConfiguredEntityProvider;
import com.akkaserverless.tck.model.eventsourcedentity.EventSourcedTckModelEntity;
import com.akkaserverless.tck.model.eventsourcedentity.EventSourcedTckModelEntityProvider;
import com.akkaserverless.tck.model.eventsourcedentity.EventSourcedTwoEntity;
import com.akkaserverless.tck.model.eventsourcedentity.EventSourcedTwoEntityProvider;
import com.akkaserverless.tck.model.valueentity.*;

import java.time.Duration;

public final class JavaSdkTck {
  public static AkkaServerless SERVICE =
      new AkkaServerless()
          .register(ActionTckModelActionProvider.of(ActionTckModelImpl::new))
          .register(ActionTwoActionProvider.of(ActionTwoImpl::new))
          .register(ValueEntityTckModelEntityProvider.of(ValueEntityTckModelEntity::new))
          .register(ValueEntityTwoEntityProvider.of(ValueEntityTwoEntity::new))
          .register(
              ValueEntityConfiguredEntityProvider.of(ValueEntityConfiguredEntity::new)
                  .withOptions(
                      ValueEntityOptions.defaults() // required timeout of 100 millis for TCK tests
                          .withPassivationStrategy(
                              PassivationStrategy.timeout(Duration.ofMillis(100)))))
          .register(ReplicatedEntityTckModelEntityProvider.of(ReplicatedEntityTckModelEntity::new))
          .register(ReplicatedEntityTwoEntityProvider.of(ReplicatedEntityTwoEntity::new))
          .register(
              ReplicatedEntityConfiguredEntityProvider.of(ReplicatedEntityConfiguredEntity::new)
                  .withOptions(
                      ReplicatedEntityOptions.defaults()
                          // required timeout of 100 millis for TCK tests
                          .withPassivationStrategy(
                              PassivationStrategy.timeout(Duration.ofMillis(100)))
                          // required write consistency for TCK tests
                          .withWriteConsistency(WriteConsistency.ALL)))
          .register(
              EventSourcedTckModelEntityProvider.of(EventSourcedTckModelEntity::new)
                  .withOptions(EventSourcedEntityOptions.defaults().withSnapshotEvery(5)))
          .register(EventSourcedTwoEntityProvider.of(EventSourcedTwoEntity::new))
          .register(
              EventSourcedConfiguredEntityProvider.of(EventSourcedConfiguredEntity::new)
                  // required timeout of 100 millis for TCK tests
                  .withOptions(
                      EventSourcedEntityOptions.defaults()
                          .withPassivationStrategy(
                              PassivationStrategy.timeout(Duration.ofMillis(100)))))
          .register(LocalPersistenceSubscriberProvider.of(LocalPersistenceSubscriber::new))
          .register(EventSourcedEntityOneProvider.of(EventSourcedEntityOne::new))
          .register(EventSourcedEntityTwoProvider.of(EventSourcedEntityTwo::new))
          .register(ValueEntityOneProvider.of(ValueEntityOne::new))
          .register(ValueEntityTwoProvider.of(ValueEntityTwo::new))
          .register(ViewTckModelBehaviorProvider.of(ViewTckModelBehavior::new))
          .register(ViewTckSourceEntityProvider.of(ViewTckSourceEntity::new));

  public static void main(String[] args) throws Exception {
    SERVICE.start();
  }
}
