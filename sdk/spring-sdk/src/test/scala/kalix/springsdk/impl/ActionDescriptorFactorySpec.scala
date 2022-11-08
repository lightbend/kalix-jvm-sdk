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
import kalix.JwtMethodOptions.JwtMethodMode
import kalix.springsdk.impl.reflection.ServiceIntrospectionException
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
import kalix.springsdk.testmodels.action.ActionsTestModels.PostWithoutParamWithJWT
import kalix.springsdk.testmodels.action.ActionsTestModels.PutWithOneParam
import kalix.springsdk.testmodels.action.ActionsTestModels.PutWithoutParam
import kalix.springsdk.testmodels.action.ActionsTestModels.StreamInAction
import kalix.springsdk.testmodels.action.ActionsTestModels.StreamInOutAction
import kalix.springsdk.testmodels.action.ActionsTestModels.StreamOutAction
import kalix.springsdk.testmodels.subscriptions.PubSubTestModels.ActionWithMethodLevelAcl
import kalix.springsdk.testmodels.subscriptions.PubSubTestModels.ActionWithMethodLevelAclAndSubscription
import kalix.springsdk.testmodels.subscriptions.PubSubTestModels.ActionWithServiceLevelAcl
import kalix.springsdk.testmodels.subscriptions.PubSubTestModels.EventStreamPublishingAction
import kalix.springsdk.testmodels.subscriptions.PubSubTestModels.EventStreamSubscriptionAction
import kalix.springsdk.testmodels.subscriptions.PubSubTestModels.InvalidSubscribeToEventSourcedEntityAction
import kalix.springsdk.testmodels.subscriptions.PubSubTestModels.InvalidSubscribeToTopicAction
import kalix.springsdk.testmodels.subscriptions.PubSubTestModels.PublishToTopicAction
import kalix.springsdk.testmodels.subscriptions.PubSubTestModels.RestAnnotatedSubscribeToEventSourcedEntityAction
import kalix.springsdk.testmodels.subscriptions.PubSubTestModels.RestAnnotatedSubscribeToValueEntityAction
import kalix.springsdk.testmodels.subscriptions.PubSubTestModels.RestWithPublishToTopicAction
import kalix.springsdk.testmodels.subscriptions.PubSubTestModels.SubscribeOnlyOneToEventSourcedEntityActionTypeLevel
import kalix.springsdk.testmodels.subscriptions.PubSubTestModels.SubscribeToEventSourcedEntityAction
import kalix.springsdk.testmodels.subscriptions.PubSubTestModels.SubscribeToTopicAction
import kalix.springsdk.testmodels.subscriptions.PubSubTestModels.SubscribeToTwoTopicsAction
import kalix.springsdk.testmodels.subscriptions.PubSubTestModels.SubscribeToValueEntityAction
import org.scalatest.wordspec.AnyWordSpec

class ActionDescriptorFactorySpec extends AnyWordSpec with ComponentDescriptorSuite {

  "Action descriptor factory" should {

    "generate mappings for an Action with GET without path param" in {
      assertDescriptor[GetWithoutParam] { desc =>

        val methodDescriptor = desc.serviceDescriptor.findMethodByName("Message")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false

        val method = desc.commandHandlers("Message")
        method.requestMessageDescriptor.getFields.size() shouldBe 0
      }
    }

    "generate mappings for an Action with GET and one path param" in {
      assertDescriptor[GetWithOneParam] { desc =>

        val methodDescriptor = desc.serviceDescriptor.findMethodByName("Message")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false

        val method = desc.commandHandlers("Message")
        assertRequestFieldJavaType(method, "one", JavaType.STRING)
      }
    }

    "generate mappings for an Action with class level Request mapping" in {
      assertDescriptor[GetClassLevel] { desc =>

        val methodDescriptor = desc.serviceDescriptor.findMethodByName("Message")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false

        val method = desc.commandHandlers("Message")
        assertRequestFieldJavaType(method, "one", JavaType.STRING)
        assertRequestFieldJavaType(method, "two", JavaType.LONG)
      }
    }

    "generate mappings for an Action with POST without path param" in {
      assertDescriptor[PostWithoutParam] { desc =>

        val methodDescriptor = desc.serviceDescriptor.findMethodByName("Message")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false

        val method = desc.commandHandlers("Message")
        assertRequestFieldJavaType(method, "json_body", JavaType.MESSAGE)
      }
    }

    "generate JWT mappings for an Action with POST" in {
      assertDescriptor[PostWithoutParamWithJWT] { desc =>
        val methodDescriptor = findMethodByName(desc, "Message")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false

        val method = desc.commandHandlers("Message")
        val jwtOption = findKalixMethodOptions(methodDescriptor).getJwt
        jwtOption.getBearerTokenIssuer(0) shouldBe "a"
        jwtOption.getBearerTokenIssuer(1) shouldBe "b"
        jwtOption.getValidate(0) shouldBe JwtMethodMode.BEARER_TOKEN
        jwtOption.getSign(0) shouldBe JwtMethodMode.MESSAGE
        assertRequestFieldJavaType(method, "json_body", JavaType.MESSAGE)

      }
    }

    "generate mappings for an Action with POST and one path param" in {
      assertDescriptor[PostWithOneParam] { desc =>

        val methodDescriptor = desc.serviceDescriptor.findMethodByName("Message")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false

        val method = desc.commandHandlers("Message")
        assertRequestFieldJavaType(method, "one", JavaType.STRING)
        assertRequestFieldJavaType(method, "json_body", JavaType.MESSAGE)
      }
    }

    "generate mappings for an Action with POST and two path param" in {
      assertDescriptor[PostWithTwoParam] { desc =>

        val methodDescriptor = desc.serviceDescriptor.findMethodByName("Message")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false

        val method = desc.commandHandlers("Message")
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

        val firstMethod = desc.commandHandlers("Message")
        assertRequestFieldJavaType(firstMethod, "num", JavaType.LONG)
        assertRequestFieldJavaType(firstMethod, "json_body", JavaType.MESSAGE)

        val methodDescriptor1 = desc.serviceDescriptor.findMethodByName("Message1")
        methodDescriptor1.isServerStreaming shouldBe false
        methodDescriptor1.isClientStreaming shouldBe false

        val secondMethod = desc.commandHandlers("Message1")
        assertRequestFieldJavaType(secondMethod, "text", JavaType.STRING)
        assertRequestFieldJavaType(secondMethod, "json_body", JavaType.MESSAGE)
      }
    }

    "generate mappings for an Action with PUT without path param" in {
      assertDescriptor[PutWithoutParam] { desc =>
        val methodDescriptor = desc.serviceDescriptor.findMethodByName("Message")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false

        val method = desc.commandHandlers("Message")
        assertRequestFieldJavaType(method, "json_body", JavaType.MESSAGE)
      }
    }

    "generate mappings for an Action with PUT and one path param" in {
      assertDescriptor[PutWithOneParam] { desc =>
        val methodDescriptor = desc.serviceDescriptor.findMethodByName("Message")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false

        val method = desc.commandHandlers("Message")
        assertRequestFieldJavaType(method, "one", JavaType.STRING)
      }
    }

    "generate mappings for an Action with PATCH without path param" in {
      assertDescriptor[PatchWithoutParam] { desc =>
        val methodDescriptor = desc.serviceDescriptor.findMethodByName("Message")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false

        val method = desc.commandHandlers("Message")
        assertRequestFieldJavaType(method, "json_body", JavaType.MESSAGE)
      }
    }

    "generate mappings for an Action with PATCH and one path param" in {
      assertDescriptor[PatchWithOneParam] { desc =>
        val methodDescriptor = desc.serviceDescriptor.findMethodByName("Message")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false

        val method = desc.commandHandlers("Message")
        assertRequestFieldJavaType(method, "one", JavaType.STRING)
      }
    }

    "generate mappings for an Action with DELETE and one path param" in {
      assertDescriptor[DeleteWithOneParam] { desc =>

        val methodDescriptor = desc.serviceDescriptor.findMethodByName("Message")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false

        val method = desc.commandHandlers("Message")
        assertRequestFieldJavaType(method, "one", JavaType.STRING)
      }
    }

    "generate combined mapping with Event Sourced Entity Subscription annotation" in {
      assertDescriptor[SubscribeToEventSourcedEntityAction] { desc =>
        val methodDescriptor = findMethodByName(desc, "KalixSyntheticMethodOnESCounter")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false

        val methodOne = desc.commandHandlers("KalixSyntheticMethodOnESCounter")
        methodOne.requestMessageDescriptor.getFullName shouldBe JavaPbAny.getDescriptor.getFullName

        val eventSourceOne = findKalixMethodOptions(methodDescriptor).getEventing.getIn
        eventSourceOne.getEventSourcedEntity shouldBe "counter"

        val ruleOne = findHttpRule(desc, "KalixSyntheticMethodOnESCounter")
        ruleOne.getPost shouldBe "/kalix.springsdk.testmodels.subscriptions.PubSubTestModels.SubscribeToEventSourcedEntityAction/KalixSyntheticMethodOnESCounter"
      }
    }

    "fail if has both Event Sourced Entity Subscription and REST annotations" in {
      intercept[IllegalArgumentException] {
        descriptorFor[RestAnnotatedSubscribeToEventSourcedEntityAction]
      }
    }

    "generate mapping with Value Entity Subscription annotations" in {
      assertDescriptor[SubscribeToValueEntityAction] { desc =>

        val methodDescriptorOne = findMethodByName(desc, "MessageOne")
        methodDescriptorOne.isServerStreaming shouldBe false
        methodDescriptorOne.isClientStreaming shouldBe false

        val methodOne = desc.commandHandlers("MessageOne")
        methodOne.requestMessageDescriptor.getFullName shouldBe JavaPbAny.getDescriptor.getFullName

        val eventSourceOne = findKalixMethodOptions(methodDescriptorOne).getEventing.getIn
        eventSourceOne.getValueEntity shouldBe "ve-counter"
        val rule = findHttpRule(desc, "MessageOne")

        rule.getPost shouldBe
        "/kalix.springsdk.testmodels.subscriptions.PubSubTestModels.SubscribeToValueEntityAction/MessageOne"

        // should have a default extractor for any payload
        val javaMethod = methodOne.methodInvokers.values.head
        javaMethod.parameterExtractors.length shouldBe 1

        val methodDescriptorTwo = findMethodByName(desc, "MessageTwo")
        methodDescriptorTwo.isServerStreaming shouldBe false
        methodDescriptorTwo.isClientStreaming shouldBe false

        val methodTwo = desc.commandHandlers("MessageTwo")
        methodTwo.requestMessageDescriptor.getFullName shouldBe JavaPbAny.getDescriptor.getFullName
        val eventSourceTwo = findKalixMethodOptions(methodDescriptorTwo).getEventing.getIn
        eventSourceTwo.getValueEntity shouldBe "ve-counter"
      }
    }

    "fail if has both Value Entity Subscription and REST annotations" in {
      intercept[IllegalArgumentException] {
        descriptorFor[RestAnnotatedSubscribeToValueEntityAction]
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
        descriptorFor[StreamInAction]
      }
    }

    "generate stream in/out methods" in {
      intercept[IllegalArgumentException] {
        descriptorFor[StreamInOutAction]
      }
    }

    "generate mapping for an Action with a subscription to a topic" in {
      assertDescriptor[SubscribeToTopicAction] { desc =>
        val methodOne = desc.commandHandlers("MessageOne")
        methodOne.requestMessageDescriptor.getFullName shouldBe JavaPbAny.getDescriptor.getFullName

        val eventSourceOne = findKalixMethodOptions(desc, "MessageOne").getEventing.getIn
        eventSourceOne.getTopic shouldBe "topicXYZ"
        eventSourceOne.getConsumerGroup shouldBe "cg"
        val rule = findHttpRule(desc, "MessageOne")

        rule.getPost shouldBe
        "/kalix.springsdk.testmodels.subscriptions.PubSubTestModels.SubscribeToTopicAction/MessageOne"

        // should have a default extractor for any payload
        val javaMethod = methodOne.methodInvokers.values.head
        javaMethod.parameterExtractors.length shouldBe 1
      }

    }

    "build a combined synthetic method when there are two subscriptions to the same topic" in {
      assertDescriptor[SubscribeToTwoTopicsAction] { desc =>
        val methodOne = desc.commandHandlers("KalixSyntheticMethodOnTopicTopicXYZ")
        methodOne.requestMessageDescriptor.getFullName shouldBe JavaPbAny.getDescriptor.getFullName

        val eventSourceOne = findKalixMethodOptions(desc, "KalixSyntheticMethodOnTopicTopicXYZ").getEventing.getIn
        eventSourceOne.getTopic shouldBe "topicXYZ"
        val rule = findHttpRule(desc, "KalixSyntheticMethodOnTopicTopicXYZ")

        rule.getPost shouldBe
        "/kalix.springsdk.testmodels.subscriptions.PubSubTestModels.SubscribeToTwoTopicsAction/KalixSyntheticMethodOnTopicTopicXYZ"

        methodOne.methodInvokers.size shouldBe 3

        val javaMethodNames = methodOne.methodInvokers.values.map(_.method.getName)
        javaMethodNames should contain("methodOne")
        javaMethodNames should contain("methodTwo")
        javaMethodNames should contain("methodThree")
      }
    }

    "generate mapping with Event Sourced Entity Subscription annotation type level with only one method" in {
      assertDescriptor[SubscribeOnlyOneToEventSourcedEntityActionTypeLevel] { desc =>
        val methodDescriptor = findMethodByName(desc, "MethodOne")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false

        val methodOne = desc.commandHandlers("MethodOne")
        methodOne.requestMessageDescriptor.getFullName shouldBe JavaPbAny.getDescriptor.getFullName

        val eventSourceOne = findKalixServiceOptions(desc).getEventing.getIn
        eventSourceOne.getEventSourcedEntity shouldBe "counter"
        // we don't set the property so the proxy won't ignore. Ignore is only internal to the SDK
        eventSourceOne.getIgnore shouldBe false
        eventSourceOne.getIgnoreUnknown shouldBe false

        val ruleOne = findHttpRule(desc, "MethodOne")
        ruleOne.getPost shouldBe "/kalix.springsdk.testmodels.subscriptions.PubSubTestModels.SubscribeOnlyOneToEventSourcedEntityActionTypeLevel/MethodOne"
      }
    }

    "validates it is forbidden Entity Subscription at annotation type level and method level at the same time" in {
      intercept[InvalidComponentException] {
        descriptorFor[InvalidSubscribeToEventSourcedEntityAction]
      }
    }

    "validates it is forbidden Topic Subscription at annotation type level and method level at the same time" in {
      intercept[InvalidComponentException] {
        descriptorFor[InvalidSubscribeToTopicAction]
      }
    }

    "generate mapping for an Action with a publication to a topic" in {
      assertDescriptor[PublishToTopicAction] { desc =>
        val methodOne = desc.commandHandlers("MessageOne")
        methodOne.requestMessageDescriptor.getFullName shouldBe JavaPbAny.getDescriptor.getFullName

        val eventDestinationOne = findKalixMethodOptions(desc, "MessageOne").getEventing.getOut
        eventDestinationOne.getTopic shouldBe "topicAlpha"
        val rule = findHttpRule(desc, "MessageOne")

        rule.getPost shouldBe
        "/kalix.springsdk.testmodels.subscriptions.PubSubTestModels.PublishToTopicAction/MessageOne"

        // should have a default extractor for any payload
        val javaMethod = methodOne.methodInvokers.values.head
        javaMethod.parameterExtractors.length shouldBe 1
      }

    }

    "generate mapping for an Action with a Rest endpoint and publication to a topic" in {
      assertDescriptor[RestWithPublishToTopicAction] { desc =>
        val methodOne = desc.commandHandlers("MessageOne")
        methodOne.requestMessageDescriptor.getFullName shouldBe "kalix.springsdk.testmodels.subscriptions.MessageOneKalixSyntheticRequest"

        val eventDestinationOne = findKalixMethodOptions(desc, "MessageOne").getEventing.getOut
        eventDestinationOne.getTopic shouldBe "foobar"
        val rule = findHttpRule(desc, "MessageOne")

        rule.getPost shouldBe
        "/message/{msg}"

        // should have a default extractor for any payload
        val javaMethod = methodOne.methodInvokers.values.head
        javaMethod.parameterExtractors.length shouldBe 1
      }

    }

    "generate ACL annotations at service level" in {
      assertDescriptor[ActionWithServiceLevelAcl] { desc =>
        val extension = desc.serviceDescriptor.getOptions.getExtension(kalix.Annotations.service)
        val service = extension.getAcl.getAllow(0).getService
        service shouldBe "test"
      }
    }

    "generate ACL annotations at method level" in {
      assertDescriptor[ActionWithMethodLevelAcl] { desc =>
        val extension = findKalixMethodOptions(desc, "MessageOne")
        val service = extension.getAcl.getAllow(0).getService
        service shouldBe "test"
      }
    }

    "fail if it's subscription method exposed with ACL" in {
      intercept[ServiceIntrospectionException] {
        descriptorFor[ActionWithMethodLevelAclAndSubscription]
      }
    }

    "generate mappings for service to service publishing " in {
      assertDescriptor[EventStreamPublishingAction] { desc =>
        val serviceOptions = findKalixServiceOptions(desc)

        val eventingOut = serviceOptions.getEventing.getOut
        eventingOut.getDirect.getEventStreamId shouldBe "employee_events"

        val methodDescriptor = findMethodByName(desc, "KalixSyntheticMethodOnESEmployee")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false

        val eventingIn = serviceOptions.getEventing.getIn
        val entityType = eventingIn.getEventSourcedEntity
        entityType shouldBe "employee"

        val methodOne = desc.commandHandlers("KalixSyntheticMethodOnESEmployee")
        methodOne.requestMessageDescriptor.getFullName shouldBe JavaPbAny.getDescriptor.getFullName

      }
    }

    "generate mappings for service to service subscription " in {
      assertDescriptor[EventStreamSubscriptionAction] { desc =>
        val serviceOptions = findKalixServiceOptions(desc)

        val eventingIn = serviceOptions.getEventing.getIn
        val eventingInDirect = eventingIn.getDirect
        eventingInDirect.getService shouldBe "employee_service"
        eventingInDirect.getEventStreamId shouldBe "employee_events"

        // we don't set the property so the proxy won't ignore. Ignore is only internal to the SDK
        eventingIn.getIgnore shouldBe false
        eventingIn.getIgnoreUnknown shouldBe false
      }
    }
  }

}
