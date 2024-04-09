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
        ValueEntityConfiguredEntityProvider(new ValueEntityConfiguredEntity(_)))
      .register(
        // required timeout of 100 millis for configured TCK tests
        EventSourcedConfiguredEntityProvider(new EventSourcedConfiguredEntity(_)))
      .register(EventSourcedTckModelEntityProvider(new EventSourcedTckModelEntity(_))
        .withOptions(EventSourcedEntityOptions.defaults.withSnapshotEvery(5)))
}
