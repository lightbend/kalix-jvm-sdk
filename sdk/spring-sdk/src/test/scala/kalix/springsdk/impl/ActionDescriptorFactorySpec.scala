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

import com.google.protobuf.Descriptors.FieldDescriptor.JavaType
import com.google.protobuf.{ Any => JavaPbAny }
import kalix.springsdk.testmodels.action.ActionsTestModels.DeleteWithOneParam
import kalix.springsdk.testmodels.action.ActionsTestModels.GetClassLevel
import kalix.springsdk.testmodels.action.ActionsTestModels.GetWithOneParam
import kalix.springsdk.testmodels.action.ActionsTestModels.GetWithoutParam
import kalix.springsdk.testmodels.action.ActionsTestModels.PatchWithOneParam
import kalix.springsdk.testmodels.action.ActionsTestModels.PatchWithoutParam
import kalix.springsdk.testmodels.action.ActionsTestModels.PostWithOneParam
import kalix.springsdk.testmodels.action.ActionsTestModels.PostWithTwoMethods
import kalix.springsdk.testmodels.action.ActionsTestModels.PostWithTwoParam
import kalix.springsdk.testmodels.action.ActionsTestModels.PostWithoutParam
import kalix.springsdk.testmodels.action.ActionsTestModels.PutWithOneParam
import kalix.springsdk.testmodels.action.ActionsTestModels.PutWithoutParam
import kalix.springsdk.testmodels.action.ActionsTestModels.StreamInAction
import kalix.springsdk.testmodels.action.ActionsTestModels.StreamInOutAction
import kalix.springsdk.testmodels.action.ActionsTestModels.StreamOutAction
import kalix.springsdk.testmodels.subscriptions.PubSubTestModels
import kalix.springsdk.testmodels.subscriptions.PubSubTestModels.{
  PublishToTopicAction,
  RestAnnotatedSubscribeToEventSourcedEntityAction,
  RestAnnotatedSubscribeToValueEntityAction,
  RestWithPublishToTopicAction,
  SubscribeToEventSourcedEntityAction,
  SubscribeToTopicAction,
  SubscribeToTwoTopicsAction,
  SubscribeToValueEntityAction
}
import org.scalatest.wordspec.AnyWordSpec

class ActionDescriptorFactorySpec extends AnyWordSpec with ComponentDescriptorSuite {

  "Action descriptor factory" should {

    "generate mappings for an Action with GET without path param" in {
      assertDescriptor[GetWithoutParam] { desc =>

        val methodDescriptor = desc.serviceDescriptor.findMethodByName("Message")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false

        val method = desc.methods("Message")
        method.requestMessageDescriptor.getFields.size() shouldBe 0
      }
    }

    "generate mappings for an Action with GET and one path param" in {
      assertDescriptor[GetWithOneParam] { desc =>

        val methodDescriptor = desc.serviceDescriptor.findMethodByName("Message")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false

        val method = desc.methods("Message")
        assertRequestFieldJavaType(method, "one", JavaType.STRING)
      }
    }

    "generate mappings for an Action with class level Request mapping" in {
      assertDescriptor[GetClassLevel] { desc =>

        val methodDescriptor = desc.serviceDescriptor.findMethodByName("Message")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false

        val method = desc.methods("Message")
        assertRequestFieldJavaType(method, "one", JavaType.STRING)
        assertRequestFieldJavaType(method, "two", JavaType.LONG)
      }
    }

    "generate mappings for an Action with POST without path param" in {
      assertDescriptor[PostWithoutParam] { desc =>

        val methodDescriptor = desc.serviceDescriptor.findMethodByName("Message")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false

        val method = desc.methods("Message")
        assertRequestFieldJavaType(method, "json_body", JavaType.MESSAGE)
      }
    }

    "generate mappings for an Action with POST and one path param" in {
      assertDescriptor[PostWithOneParam] { desc =>

        val methodDescriptor = desc.serviceDescriptor.findMethodByName("Message")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false

        val method = desc.methods("Message")
        assertRequestFieldJavaType(method, "one", JavaType.STRING)
        assertRequestFieldJavaType(method, "json_body", JavaType.MESSAGE)
      }
    }

    "generate mappings for an Action with POST and two path param" in {
      assertDescriptor[PostWithTwoParam] { desc =>

        val methodDescriptor = desc.serviceDescriptor.findMethodByName("Message")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false

        val method = desc.methods("Message")
        assertRequestFieldJavaType(method, "one", JavaType.STRING)
        assertRequestFieldJavaType(method, "two", JavaType.LONG)
        assertRequestFieldJavaType(method, "json_body", JavaType.MESSAGE)
      }
    }

    "generate mappings for an Action with POST and two methods" in {
      assertDescriptor[PostWithTwoMethods] { desc =>

        val methodDescriptor = desc.serviceDescriptor.findMethodByName("Message")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false

        val firstMethod = desc.methods("Message")
        assertRequestFieldJavaType(firstMethod, "num", JavaType.LONG)
        assertRequestFieldJavaType(firstMethod, "json_body", JavaType.MESSAGE)

        val methodDescriptor1 = desc.serviceDescriptor.findMethodByName("Message1")
        methodDescriptor1.isServerStreaming shouldBe false
        methodDescriptor1.isClientStreaming shouldBe false

        val secondMethod = desc.methods("Message1")
        assertRequestFieldJavaType(secondMethod, "text", JavaType.STRING)
        assertRequestFieldJavaType(secondMethod, "json_body", JavaType.MESSAGE)
      }
    }

    "generate mappings for an Action with PUT without path param" in {
      assertDescriptor[PutWithoutParam] { desc =>
        val methodDescriptor = desc.serviceDescriptor.findMethodByName("Message")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false

        val method = desc.methods("Message")
        assertRequestFieldJavaType(method, "json_body", JavaType.MESSAGE)
      }
    }

    "generate mappings for an Action with PUT and one path param" in {
      assertDescriptor[PutWithOneParam] { desc =>
        val methodDescriptor = desc.serviceDescriptor.findMethodByName("Message")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false

        val method = desc.methods("Message")
        assertRequestFieldJavaType(method, "one", JavaType.STRING)
      }
    }

    "generate mappings for an Action with PATCH without path param" in {
      assertDescriptor[PatchWithoutParam] { desc =>
        val methodDescriptor = desc.serviceDescriptor.findMethodByName("Message")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false

        val method = desc.methods("Message")
        assertRequestFieldJavaType(method, "json_body", JavaType.MESSAGE)
      }
    }

    "generate mappings for an Action with PATCH and one path param" in {
      assertDescriptor[PatchWithOneParam] { desc =>
        val methodDescriptor = desc.serviceDescriptor.findMethodByName("Message")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false

        val method = desc.methods("Message")
        assertRequestFieldJavaType(method, "one", JavaType.STRING)
      }
    }

    "generate mappings for an Action with DELETE and one path param" in {
      assertDescriptor[DeleteWithOneParam] { desc =>

        val methodDescriptor = desc.serviceDescriptor.findMethodByName("Message")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false

        val method = desc.methods("Message")
        assertRequestFieldJavaType(method, "one", JavaType.STRING)
      }
    }
    "generate mappint with Event Sourced Entity Subscription annotation" in {
      assertDescriptor[SubscribeToEventSourcedEntityAction] { desc =>
        val methodDescriptorOne = desc.serviceDescriptor.findMethodByName("MethodOne")
        methodDescriptorOne.isServerStreaming shouldBe false
        methodDescriptorOne.isClientStreaming shouldBe false

        val methodOne = desc.methods("MethodOne")
        methodOne.requestMessageDescriptor.getFullName shouldBe JavaPbAny.getDescriptor.getFullName

        val eventSourceOne = findKalixMethodOptions(desc, "MethodOne").getEventing.getIn
        eventSourceOne.getEventSourcedEntity shouldBe "counter"

        val ruleOne = findHttpRule(desc, "MethodOne")
        ruleOne.getPost shouldBe "/kalix.springsdk.testmodels.subscriptions.PubSubTestModels.SubscribeToEventSourcedEntityAction/MethodOne"

        val methodDescriptorTwo = desc.serviceDescriptor.findMethodByName("MethodTwo")
        methodDescriptorTwo.isServerStreaming shouldBe false
        methodDescriptorTwo.isClientStreaming shouldBe false

        val methodTwo = desc.methods("MethodTwo")
        methodTwo.requestMessageDescriptor.getFullName shouldBe JavaPbAny.getDescriptor.getFullName

        val eventSourceTwo = findKalixMethodOptions(desc, "MethodTwo").getEventing.getIn
        eventSourceTwo.getEventSourcedEntity shouldBe "counter"

        val ruleTwo = findHttpRule(desc, "MethodTwo")
        ruleTwo.getPost shouldBe "/kalix.springsdk.testmodels.subscriptions.PubSubTestModels.SubscribeToEventSourcedEntityAction/MethodTwo"

      }
    }

    "fail if has both Event Sourced Entity Subscription and REST annotations" in {
      intercept[IllegalArgumentException] {
        ComponentDescriptor.descriptorFor[RestAnnotatedSubscribeToEventSourcedEntityAction]
      }
    }

    "generate mapping with Value Entity Subscription annotations" in {
      assertDescriptor[SubscribeToValueEntityAction] { desc =>

        val methodDescriptorOne = desc.serviceDescriptor.findMethodByName("MessageOne")
        methodDescriptorOne.isServerStreaming shouldBe false
        methodDescriptorOne.isClientStreaming shouldBe false

        val methodOne = desc.methods("MessageOne")
        methodOne.requestMessageDescriptor.getFullName shouldBe JavaPbAny.getDescriptor.getFullName

        val eventSourceOne = findKalixMethodOptions(desc, "MessageOne").getEventing.getIn
        eventSourceOne.getValueEntity shouldBe "ve-counter"
        val rule = findHttpRule(desc, "MessageOne")

        rule.getPost shouldBe
        "/kalix.springsdk.testmodels.subscriptions.PubSubTestModels.SubscribeToValueEntityAction/MessageOne"

        // should have a default extractor for any payload
        methodOne.parameterExtractors.size shouldBe 1

        val methodDescriptorTwo = desc.serviceDescriptor.findMethodByName("MessageTwo")
        methodDescriptorTwo.isServerStreaming shouldBe false
        methodDescriptorTwo.isClientStreaming shouldBe false

        val methodTwo = desc.methods("MessageTwo")
        methodTwo.requestMessageDescriptor.getFullName shouldBe JavaPbAny.getDescriptor.getFullName
        val eventSourceTwo = findKalixMethodOptions(desc, "MessageTwo").getEventing.getIn
        eventSourceTwo.getValueEntity shouldBe "ve-counter"
      }
    }

    "fail if has both Value Entity Subscription and REST annotations" in {
      intercept[IllegalArgumentException] {
        ComponentDescriptor.descriptorFor[RestAnnotatedSubscribeToValueEntityAction]
      }
    }

    "generate stream out methods" in {
      assertDescriptor[StreamOutAction] { desc =>
        val methodDescriptor = desc.serviceDescriptor.findMethodByName("Message")
        methodDescriptor.isServerStreaming shouldBe true
        methodDescriptor.isClientStreaming shouldBe false
      }
    }

    "generate stream in methods" in {
      intercept[IllegalArgumentException] {
        ComponentDescriptor.descriptorFor[StreamInAction]
      }
    }

    "generate stream in/out methods" in {
      intercept[IllegalArgumentException] {
        ComponentDescriptor.descriptorFor[StreamInOutAction]
      }
    }
    "generate mapping for an Action with a subscription to a topic" in {
      assertDescriptor[SubscribeToTopicAction] { desc =>
        val methodOne = desc.methods("MessageOne")
        methodOne.requestMessageDescriptor.getFullName shouldBe JavaPbAny.getDescriptor.getFullName

        val eventSourceOne = findKalixMethodOptions(desc, "MessageOne").getEventing.getIn
        eventSourceOne.getTopic shouldBe "topicXYZ"
        eventSourceOne.getConsumerGroup shouldBe "cg"
        val rule = findHttpRule(desc, "MessageOne")

        rule.getPost shouldBe
        "/kalix.springsdk.testmodels.subscriptions.PubSubTestModels.SubscribeToTopicAction/MessageOne"

        // should have a default extractor for any payload
        methodOne.parameterExtractors.size shouldBe 1
      }

    }

    "Raise and error when there are two subscriptions to the same topic" in {
      intercept[InvalidComponentException] {
        ComponentDescriptor.descriptorFor[SubscribeToTwoTopicsAction]
      }
    }

    "generate mapping for an Action with a publication to a topic" in {
      assertDescriptor[PublishToTopicAction] { desc =>
        val methodOne = desc.methods("MessageOne")
        methodOne.requestMessageDescriptor.getFullName shouldBe JavaPbAny.getDescriptor.getFullName

        val eventDestinationOne = findKalixMethodOptions(desc, "MessageOne").getEventing.getOut
        eventDestinationOne.getTopic shouldBe "topicAlpha"
        val rule = findHttpRule(desc, "MessageOne")

        rule.getPost shouldBe
        "/kalix.springsdk.testmodels.subscriptions.PubSubTestModels.PublishToTopicAction/MessageOne"

        // should have a default extractor for any payload
        methodOne.parameterExtractors.size shouldBe 1
      }

    }

    "generate mapping for an Action with a Rest endpoint and publication to a topic" in {
      assertDescriptor[RestWithPublishToTopicAction] { desc =>
        val methodOne = desc.methods("MessageOne")
        methodOne.requestMessageDescriptor.getFullName shouldBe "kalix.springsdk.testmodels.subscriptions.MessageOneKalixSyntheticRequest"

        val eventDestinationOne = findKalixMethodOptions(desc, "MessageOne").getEventing.getOut
        eventDestinationOne.getTopic shouldBe "foobar"
        val rule = findHttpRule(desc, "MessageOne")

        rule.getPost shouldBe
        "/message/{msg}"

        // should have a default extractor for any payload
        methodOne.parameterExtractors.size shouldBe 1
      }

    }
  }

}
