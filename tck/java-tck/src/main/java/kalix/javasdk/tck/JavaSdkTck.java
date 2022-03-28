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
import com.akkaserverless.javasdk.tck.model.localpersistenceeventing.ValueEntityTwo;
import com.akkaserverless.javasdk.tck.model.localpersistenceeventing.*;
import com.akkaserverless.javasdk.tck.model.replicatedentity.*;
import com.akkaserverless.javasdk.valueentity.ValueEntityOptions;
import com.akkaserverless.tck.model.action.ActionTckModelActionProvider;
import com.akkaserverless.tck.model.action.ActionTckModelImpl;
import com.akkaserverless.tck.model.action.ActionTwoActionProvider;
import com.akkaserverless.tck.model.action.ActionTwoImpl;
import com.akkaserverless.tck.model.eventsourcedentity.*;
import com.akkaserverless.tck.model.valueentity.*;
import com.akkaserverless.tck.model.view.ViewTckModelImpl;
import com.akkaserverless.tck.model.view.ViewTckModelViewProvider;
import com.akkaserverless.tck.model.view.ViewTckSourceEntity;
import com.akkaserverless.tck.model.view.ViewTckSourceEntityProvider;

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
          .register(ViewTckModelViewProvider.of(ViewTckModelImpl::new))
          .register(ViewTckSourceEntityProvider.of(ViewTckSourceEntity::new));

  public static void main(String[] args) throws Exception {
    SERVICE.start();
  }
}
