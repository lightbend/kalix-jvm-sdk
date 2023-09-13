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

package kalix.javasdk.testkit.impl

import kalix.javasdk.testkit.KalixTestKit.MockedEventing
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class KalixTestKitSpec extends AnyWordSpec with Matchers {

  "MockedSubscriptions" should {
    "create config" in {
      val config = MockedEventing.EMPTY
        .withMockedValueEntitySubscription("a")
        .withMockedValueEntitySubscription("b")
        .withMockedEventSourcedSubscription("c")
        .withMockedEventSourcedSubscription("d")
        .withMockedStreamSubscription("s1", "e")
        .withMockedStreamSubscription("s2", "f")
        .withMockedTopicSubscription("g")
        .withMockedTopicSubscription("h")
        .toSubscriptionsConfig

      config shouldBe "value-entity,a;value-entity,b;event-sourced-entity,c;event-sourced-entity,d;stream,s1/e;stream,s2/f;topic,g;topic,h"
    }
  }

}
