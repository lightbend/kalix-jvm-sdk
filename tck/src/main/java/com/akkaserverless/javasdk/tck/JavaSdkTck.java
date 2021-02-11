/*
 * Copyright 2019 Lightbend Inc.
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
import com.akkaserverless.javasdk.crdt.CrdtEntityOptions;
import com.akkaserverless.javasdk.entity.EntityOptions;
import com.akkaserverless.javasdk.eventsourced.EventSourcedEntityOptions;
import com.akkaserverless.javasdk.tck.model.action.ActionTckModelBehavior;
import com.akkaserverless.javasdk.tck.model.action.ActionTwoBehavior;
import com.akkaserverless.javasdk.tck.model.crdt.CrdtConfiguredEntity;
import com.akkaserverless.javasdk.tck.model.crdt.CrdtTckModelEntity;
import com.akkaserverless.javasdk.tck.model.crdt.CrdtTwoEntity;
import com.akkaserverless.javasdk.tck.model.eventlogeventing.EventLogSubscriber;
import com.akkaserverless.javasdk.tck.model.eventsourced.EventSourcedConfiguredEntity;
import com.akkaserverless.javasdk.tck.model.eventsourced.EventSourcedTckModelEntity;
import com.akkaserverless.javasdk.tck.model.eventsourced.EventSourcedTwoEntity;
import com.akkaserverless.javasdk.tck.model.valuebased.ValueEntityConfiguredEntity;
import com.akkaserverless.javasdk.tck.model.valuebased.ValueEntityTckModelEntity;
import com.akkaserverless.javasdk.tck.model.valuebased.ValueEntityTwoEntity;
import com.akkaserverless.tck.model.Action;
import com.akkaserverless.tck.model.Crdt;
import com.akkaserverless.tck.model.Eventlogeventing;
import com.akkaserverless.tck.model.Eventsourced;
import com.akkaserverless.tck.model.valueentity.Valueentity;

import java.time.Duration;

public final class JavaSdkTck {
  public static final void main(String[] args) throws Exception {
    new AkkaServerless()
        .registerAction(
            new ActionTckModelBehavior(),
            Action.getDescriptor().findServiceByName("ActionTckModel"),
            Action.getDescriptor())
        .registerAction(
            new ActionTwoBehavior(),
            Action.getDescriptor().findServiceByName("ActionTwo"),
            Action.getDescriptor())
        .registerEntity(
            ValueEntityTckModelEntity.class,
            Valueentity.getDescriptor().findServiceByName("ValueEntityTckModel"),
            Valueentity.getDescriptor())
        .registerEntity(
            ValueEntityTwoEntity.class,
            Valueentity.getDescriptor().findServiceByName("ValueEntityTwo"))
        .registerEntity(
            ValueEntityConfiguredEntity.class,
            Valueentity.getDescriptor().findServiceByName("ValueEntityConfigured"),
            EntityOptions.defaults() // required timeout of 100 millis for TCK tests
                .withPassivationStrategy(PassivationStrategy.timeout(Duration.ofMillis(100))))
        .registerCrdtEntity(
            CrdtTckModelEntity.class,
            Crdt.getDescriptor().findServiceByName("CrdtTckModel"),
            Crdt.getDescriptor())
        .registerCrdtEntity(CrdtTwoEntity.class, Crdt.getDescriptor().findServiceByName("CrdtTwo"))
        .registerCrdtEntity(
            CrdtConfiguredEntity.class,
            Crdt.getDescriptor().findServiceByName("CrdtConfigured"),
            CrdtEntityOptions.defaults() // required timeout of 100 millis for TCK tests
                .withPassivationStrategy(PassivationStrategy.timeout(Duration.ofMillis(100))))
        .registerEventSourcedEntity(
            EventSourcedTckModelEntity.class,
            Eventsourced.getDescriptor().findServiceByName("EventSourcedTckModel"),
            Eventsourced.getDescriptor())
        .registerEventSourcedEntity(
            EventSourcedTwoEntity.class,
            Eventsourced.getDescriptor().findServiceByName("EventSourcedTwo"))
        .registerEventSourcedEntity(
            EventSourcedConfiguredEntity.class,
            Eventsourced.getDescriptor().findServiceByName("EventSourcedConfigured"),
            EventSourcedEntityOptions.defaults() // required timeout of 100 millis for TCK tests
                .withPassivationStrategy(PassivationStrategy.timeout(Duration.ofMillis(100))))
        .registerAction(
            new EventLogSubscriber(),
            Eventlogeventing.getDescriptor().findServiceByName("EventLogSubscriberModel"))
        .registerEventSourcedEntity(
            com.akkaserverless.javasdk.tck.model.eventlogeventing.EventSourcedEntityOne.class,
            Eventlogeventing.getDescriptor().findServiceByName("EventSourcedEntityOne"),
            Eventlogeventing.getDescriptor())
        .registerEventSourcedEntity(
            com.akkaserverless.javasdk.tck.model.eventlogeventing.EventSourcedEntityTwo.class,
            Eventlogeventing.getDescriptor().findServiceByName("EventSourcedEntityTwo"),
            Eventlogeventing.getDescriptor())
        .start()
        .toCompletableFuture()
        .get();
  }
}
