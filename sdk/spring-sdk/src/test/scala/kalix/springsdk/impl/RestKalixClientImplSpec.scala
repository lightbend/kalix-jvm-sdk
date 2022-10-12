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

import com.google.protobuf.any.Any
import kalix.javasdk.impl.RestDeferredCallImpl
import kalix.springsdk.KalixClient
import kalix.springsdk.testmodels.action.ActionsTestModels.GetWithoutParam
import kalix.springsdk.testmodels.{ Message, NestedMessage, SimpleMessage }
import org.scalatest.{ BeforeAndAfter, BeforeAndAfterAll }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class RestKalixClientImplSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with ComponentDescriptorSuite {
  val restKalixClient: RestKalixClientImpl = new RestKalixClientImpl(new SpringSdkMessageCodec)
  val actionDesc = ComponentDescriptor.descriptorFor[GetWithoutParam]

  override def beforeAll(): Unit = {

    restKalixClient.registerComponent(actionDesc.serviceDescriptor)
  }

  "The Rest Kalix Client" should {
    "return a DeferredCall for a POST request" in {
      val defCall = restKalixClient.get("/message", classOf[Message])
      defCall shouldBe a[RestDeferredCallImpl[Any, _]]

      val restDefCall = defCall.asInstanceOf[RestDeferredCallImpl[Any, Message]]
      val targetMethod = actionDesc.serviceDescriptor.findMethodByName("Message")
      restDefCall.message.typeUrl shouldBe targetMethod.getInputType.getFullName

      //FIXME to be continued
    }

    "return a DeferredCall for a GET request" in {}
  }

}
