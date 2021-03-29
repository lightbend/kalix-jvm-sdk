/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.tck;

import com.akkaserverless.javasdk.AkkaServerless;
import com.akkaserverless.javasdk.PassivationStrategy;
import com.akkaserverless.javasdk.crdt.CrdtEntityOptions;
import com.akkaserverless.javasdk.valueentity.ValueEntityOptions;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityOptions;
import com.akkaserverless.javasdk.tck.model.action.ActionTckModelBehavior;
import com.akkaserverless.javasdk.tck.model.action.ActionTwoBehavior;
import com.akkaserverless.javasdk.tck.model.crdt.CrdtConfiguredEntity;
import com.akkaserverless.javasdk.tck.model.crdt.CrdtTckModelEntity;
import com.akkaserverless.javasdk.tck.model.crdt.CrdtTwoEntity;
import com.akkaserverless.javasdk.tck.model.localpersistenceeventing.LocalPersistenceSubscriber;
import com.akkaserverless.javasdk.tck.model.eventsourcedentity.EventSourcedConfiguredEntity;
import com.akkaserverless.javasdk.tck.model.eventsourcedentity.EventSourcedTckModelEntity;
import com.akkaserverless.javasdk.tck.model.eventsourcedentity.EventSourcedTwoEntity;
import com.akkaserverless.javasdk.tck.model.valueentity.ValueEntityConfiguredEntity;
import com.akkaserverless.javasdk.tck.model.valueentity.ValueEntityTckModelEntity;
import com.akkaserverless.javasdk.tck.model.valueentity.ValueEntityTwoEntity;
import com.akkaserverless.tck.model.Action;
import com.akkaserverless.tck.model.CrdtEntity;
import com.akkaserverless.tck.model.eventing.LocalPersistenceEventing;
import com.akkaserverless.tck.model.EventSourcedEntity;
import com.akkaserverless.tck.model.ValueEntity;

import java.time.Duration;

public final class JavaSdkTck {
  public static void main(String[] args) throws Exception {
    new AkkaServerless()
        .registerAction(
            new ActionTckModelBehavior(),
            Action.getDescriptor().findServiceByName("ActionTckModel"),
            Action.getDescriptor())
        .registerAction(
            new ActionTwoBehavior(),
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
        .registerCrdtEntity(
            CrdtTckModelEntity.class,
            CrdtEntity.getDescriptor().findServiceByName("CrdtTckModel"),
            CrdtEntity.getDescriptor())
        .registerCrdtEntity(
            CrdtTwoEntity.class, CrdtEntity.getDescriptor().findServiceByName("CrdtTwo"))
        .registerCrdtEntity(
            CrdtConfiguredEntity.class,
            CrdtEntity.getDescriptor().findServiceByName("CrdtConfigured"),
            CrdtEntityOptions.defaults() // required timeout of 100 millis for TCK tests
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
            new LocalPersistenceSubscriber(),
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
        .start()
        .toCompletableFuture()
        .get();
  }
}
