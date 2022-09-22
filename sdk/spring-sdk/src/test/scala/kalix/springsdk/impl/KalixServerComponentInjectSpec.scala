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

import kalix.springsdk.badwiring.ActionWithAnotherAction
import kalix.springsdk.badwiring.ActionWithEventSourcedEntity
import kalix.springsdk.badwiring.ActionWithValueEntity
import kalix.springsdk.badwiring.ActionWithView
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.springframework.beans.factory.BeanCreationException

class KalixServerComponentInjectSpec extends AnyWordSpec with Matchers {

  "The KalixServer" should {
    "not allow Kalix components depend on other components" in {

      val errorMessage =
        intercept[BeanCreationException] {
          kalix.springsdk.badwiring.Main.main(Array.empty)
        }.getCause.getCause.getMessage

      // those are all badly defined actions, each receiving of the existing components
      // all should fail and be reported
      errorMessage should include(classOf[ActionWithAnotherAction].getName)
      errorMessage should include(classOf[ActionWithEventSourcedEntity].getName)
      errorMessage should include(classOf[ActionWithValueEntity].getName)
      errorMessage should include(classOf[ActionWithView].getName)

    }
  }

}
