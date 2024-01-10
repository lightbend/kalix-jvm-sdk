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

package kalix.javasdk.testkit.impl

import kalix.javasdk.testkit.KalixTestKit.MockedEventing
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class KalixTestKitSpec extends AnyWordSpec with Matchers {

  "MockedSubscriptions" should {
    "create config" in {
      val config = MockedEventing.EMPTY
        .withValueEntityIncomingMessages("a")
        .withValueEntityIncomingMessages("b")
        .withEventSourcedIncomingMessages("c")
        .withEventSourcedIncomingMessages("d")
        .withStreamIncomingMessages("s1", "e")
        .withStreamIncomingMessages("s2", "f")
        .withTopicIncomingMessages("g")
        .withTopicIncomingMessages("h")
        .withTopicOutgoingMessages("aa")
        .withTopicOutgoingMessages("bb")

      config.toIncomingFlowConfig shouldBe "event-sourced-entity,c;event-sourced-entity,d;stream,s1/e;stream,s2/f;topic,g;topic,h;value-entity,a;value-entity,b"
      config.toOutgoingFlowConfig shouldBe "topic,aa;topic,bb"
    }

    "create immutable config" in {
      val config = MockedEventing.EMPTY
      val updatedConfig = MockedEventing.EMPTY.withTopicOutgoingMessages("test")

      config.hasIncomingConfig shouldBe false
      config.hasOutgoingConfig shouldBe false

      updatedConfig.hasIncomingConfig
      updatedConfig.toOutgoingFlowConfig shouldBe "topic,test"
    }
  }

}
