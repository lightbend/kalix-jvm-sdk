/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.tck.model

import kalix.scalasdk.eventsourcedentity.EventSourcedEntityOptions
import kalix.tck.model.eventsourcedentity.EventSourcedConfiguredEntity
import kalix.tck.model.eventsourcedentity.EventSourcedConfiguredEntityProvider
import kalix.tck.model.eventsourcedentity.EventSourcedTckModelEntity
import kalix.tck.model.eventsourcedentity.EventSourcedTckModelEntityProvider
import kalix.tck.model.valueentity.ValueEntityConfiguredEntity
import kalix.tck.model.valueentity.ValueEntityConfiguredEntityProvider

/**
 * Create the Kalix instance with some required configuration changes.
 *
 * This construction allows to regenerate the Main class automatically and use it as is.
 */
object TckService {
  def createService() =
    Main
      .createKalix()
      // take Main registrations and override a few ones with extra options
      .register(
        // required timeout of 100 millis for configured TCK tests
        ValueEntityConfiguredEntityProvider(_ => new ValueEntityConfiguredEntity))
      .register(
        // required timeout of 100 millis for configured TCK tests
        EventSourcedConfiguredEntityProvider(_ => new EventSourcedConfiguredEntity))
      .register(EventSourcedTckModelEntityProvider(_ => new EventSourcedTckModelEntity)
        .withOptions(EventSourcedEntityOptions.defaults.withSnapshotEvery(5)))
}
