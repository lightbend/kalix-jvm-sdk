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

package kalix.springsdk.impl

import kalix.springsdk.badwiring.action
import kalix.springsdk.badwiring.action.IllDefinedAction
import kalix.springsdk.badwiring.eventsourced
import kalix.springsdk.badwiring.eventsourced.IllDefinedEventSourcedEntity
import kalix.springsdk.badwiring.valueentity
import kalix.springsdk.badwiring.valueentity.IllDefinedValueEntity
import kalix.springsdk.badwiring.view
import kalix.springsdk.badwiring.view.IllDefinedView
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.springframework.beans.factory.BeanCreationException

class KalixComponentInjectionBlockerSpec extends AnyWordSpec with Matchers {

  "The KalixComponentInjectionBlocker" should {

    "block direct wiring of Kalix Actions" in {
      val errorMessage =
        intercept[BeanCreationException] {
          action.Main.main(Array.empty)
        }.getCause.getMessage

      errorMessage should include(classOf[IllDefinedAction].getName)
    }

    "block direct wiring of Kalix Event Sourced entities" in {
      val errorMessage =
        intercept[BeanCreationException] {
          eventsourced.Main.main(Array.empty)
        }.getCause.getMessage

      errorMessage should include(classOf[IllDefinedEventSourcedEntity].getName)
    }

    "block direct wiring of Kalix Value entities" in {
      val errorMessage =
        intercept[BeanCreationException] {
          valueentity.Main.main(Array.empty)
        }.getCause.getMessage

      errorMessage should include(classOf[IllDefinedValueEntity].getName)
    }

    "block direct wiring of Kalix Views" in {
      val errorMessage =
        intercept[BeanCreationException] {
          view.Main.main(Array.empty)
        }.getCause.getMessage

      errorMessage should include(classOf[IllDefinedView].getName)
    }
  }

}
