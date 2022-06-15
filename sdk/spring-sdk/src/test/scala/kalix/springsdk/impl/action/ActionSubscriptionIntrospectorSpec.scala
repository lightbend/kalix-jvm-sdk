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

package kalix.springsdk.impl.action

import com.google.protobuf.{ Any => JavaPbAny }
import kalix.springsdk.impl.IntrospectionSuite
import kalix.springsdk.testmodels.subscriptions.SubscriptionsTestModels.SubscribeToValueEntityAction
import org.scalatest.wordspec.AnyWordSpec

class ActionSubscriptionIntrospectorSpec extends AnyWordSpec with IntrospectionSuite {

  "generate mapping with value entity subscription annotations" in {
    assertDescriptor[SubscribeToValueEntityAction] { desc =>

      val methodOne = desc.methods("MessageOne")
      methodOne.messageDescriptor.getFullName shouldBe JavaPbAny.getDescriptor.getFullName

      val eventSourceOne = findEventSource(desc, "MessageOne")
      eventSourceOne.getValueEntity shouldBe "ve-counter"

      val methodTwo = desc.methods("MessageTwo")
      methodTwo.messageDescriptor.getFullName shouldBe JavaPbAny.getDescriptor.getFullName
      val eventSourceTwo = findEventSource(desc, "MessageTwo")
      eventSourceTwo.getValueEntity shouldBe "ve-counter"
    }
  }
}
