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

package kalix.javasdk.tck;

import kalix.javasdk.Kalix;
import kalix.javasdk.PassivationStrategy;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityOptions;
import kalix.javasdk.replicatedentity.ReplicatedEntityOptions;
import kalix.javasdk.replicatedentity.WriteConsistency;
import kalix.javasdk.tck.model.localpersistenceeventing.ValueEntityTwo;
import kalix.javasdk.tck.model.localpersistenceeventing.*;
import kalix.javasdk.tck.model.replicatedentity.*;
import kalix.javasdk.valueentity.ValueEntityOptions;
import kalix.tck.model.action.ActionTckModelActionProvider;
import kalix.tck.model.action.ActionTckModelImpl;
import kalix.tck.model.action.ActionTwoActionProvider;
import kalix.tck.model.action.ActionTwoImpl;
import kalix.tck.model.eventsourcedentity.*;
import kalix.tck.model.valueentity.*;
import kalix.tck.model.view.ViewTckModelImpl;
import kalix.tck.model.view.ViewTckModelViewProvider;
import kalix.tck.model.view.ViewTckSourceEntity;
import kalix.tck.model.view.ViewTckSourceEntityProvider;

import java.time.Duration;

public final class JavaSdkTck {
  public static Kalix SERVICE =
      new Kalix()
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
