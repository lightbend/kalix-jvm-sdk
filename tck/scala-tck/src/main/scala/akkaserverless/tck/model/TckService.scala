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

package akkaserverless.tck.model

import scala.concurrent.duration._

import com.akkaserverless.scalasdk.PassivationStrategy
import com.akkaserverless.scalasdk.valueentity.ValueEntityOptions
import com.akkaserverless.tck.model.valueentity.{ ValueEntityConfiguredEntity, ValueEntityConfiguredEntityProvider }

/** Create the AkkaServerless instance with some required configuration changes */
object TckService {
  def createService() =
    Main
      .createAkkaServerless()
      .register(
        ValueEntityConfiguredEntityProvider(new ValueEntityConfiguredEntity(_))
          .withOptions(ValueEntityOptions.defaults.withPassivationStrategy(PassivationStrategy.timeout(100.millis))))
}
