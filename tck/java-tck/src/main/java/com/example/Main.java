/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example;

import kalix.javasdk.Kalix;
import kalix.tck.model.action.ActionTckModelImpl;
import kalix.tck.model.action.ActionTwoImpl;
import kalix.tck.model.eventsourcedentity.EventSourcedConfiguredEntity;
import kalix.tck.model.eventsourcedentity.EventSourcedTckModelEntity;
import kalix.tck.model.eventsourcedentity.EventSourcedTwoEntity;
import kalix.tck.model.valueentity.ValueEntityConfiguredEntity;
import kalix.tck.model.valueentity.ValueEntityTckModelEntity;
import kalix.tck.model.valueentity.ValueEntityTwoEntity;
import kalix.tck.model.view.ViewTckModelImpl;
import kalix.tck.model.view.ViewTckSourceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public final class Main {

  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  public static Kalix createKalix() {
    // The KalixFactory automatically registers any generated Actions, Views or Entities,
    // and is kept up-to-date with any changes in your protobuf definitions.
    // If you prefer, you may remove this and manually register these components in a
    // `new Kalix()` instance.
    return KalixFactory.withComponents(
        EventSourcedConfiguredEntity::new,
        EventSourcedTckModelEntity::new,
        EventSourcedTwoEntity::new,
        ValueEntityConfiguredEntity::new,
        ValueEntityTckModelEntity::new,
        ValueEntityTwoEntity::new,
        ViewTckSourceEntity::new,
        ActionTckModelImpl::new,
        ActionTwoImpl::new,
        ViewTckModelImpl::new);
  }

  public static void main(String[] args) throws Exception {
    LOG.info("starting the Kalix service");
    createKalix().start();
  }
}
