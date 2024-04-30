/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.tck.model

import kalix.scalasdk.Kalix
import kalix.tck.model.action.ActionTckModelImpl
import kalix.tck.model.action.ActionTwoImpl
import kalix.tck.model.eventsourcedentity.EventSourcedConfiguredEntity
import kalix.tck.model.eventsourcedentity.EventSourcedTckModelEntity
import kalix.tck.model.eventsourcedentity.EventSourcedTwoEntity
import kalix.tck.model.valueentity.ValueEntityConfiguredEntity
import kalix.tck.model.valueentity.ValueEntityTckModelEntity
import kalix.tck.model.valueentity.ValueEntityTwoEntity
import kalix.tck.model.view.ViewTckModelImpl
import kalix.tck.model.view.ViewTckSourceEntity
import org.slf4j.LoggerFactory

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

object Main {

  private val log = LoggerFactory.getLogger("kalix.tck.model.Main")

  def createKalix(): Kalix = {
    // The KalixFactory automatically registers any generated Actions, Views or Entities,
    // and is kept up-to-date with any changes in your protobuf definitions.
    // If you prefer, you may remove this and manually register these components in a
    // `Kalix()` instance.
    KalixFactory.withComponents(
      _ => new EventSourcedConfiguredEntity,
      _ => new EventSourcedTckModelEntity,
      _ => new EventSourcedTwoEntity,
      _ => new ValueEntityConfiguredEntity,
      _ => new ValueEntityTckModelEntity,
      _ => new ValueEntityTwoEntity,
      _ => new ViewTckSourceEntity,
      new ActionTckModelImpl(_),
      _ => new ActionTwoImpl(),
      _ => new ViewTckModelImpl)
  }

  def main(args: Array[String]): Unit = {
    log.info("starting the Kalix service")
    createKalix().start()
  }
}
