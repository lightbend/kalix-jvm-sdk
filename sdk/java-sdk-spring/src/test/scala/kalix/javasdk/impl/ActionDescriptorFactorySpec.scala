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

package kalix.javasdk.impl

import com.google.protobuf.Descriptors.FieldDescriptor.JavaType
import com.google.protobuf.empty.Empty
import com.google.protobuf.{ Any => JavaPbAny }
import kalix.JwtMethodOptions.JwtMethodMode
import kalix.javasdk.impl.reflection.ServiceIntrospectionException
import kalix.spring.testmodels.action.ActionsTestModels.DeleteWithOneParam
import kalix.spring.testmodels.action.ActionsTestModels.GetClassLevel
import kalix.spring.testmodels.action.ActionsTestModels.GetWithOneOptionalPathParam
import kalix.spring.testmodels.action.ActionsTestModels.GetWithOneOptionalQueryParam
import kalix.spring.testmodels.action.ActionsTestModels.GetWithOneParam
import kalix.spring.testmodels.action.ActionsTestModels.GetWithOnePathVariableAndQueryParam
import kalix.spring.testmodels.action.ActionsTestModels.GetWithOneQueryParam
import kalix.spring.testmodels.action.ActionsTestModels.GetWithoutParam
import kalix.spring.testmodels.action.ActionsTestModels.PatchWithOneParam
import kalix.spring.testmodels.action.ActionsTestModels.PatchWithoutParam
import kalix.spring.testmodels.action.ActionsTestModels.PostWithOneParam
import kalix.spring.testmodels.action.ActionsTestModels.PostWithTwoMethods
import kalix.spring.testmodels.action.ActionsTestModels.PostWithTwoParam
import kalix.spring.testmodels.action.ActionsTestModels.PostWithoutParam
import kalix.spring.testmodels.action.ActionsTestModels.PostWithoutParamWithJWT
import kalix.spring.testmodels.action.ActionsTestModels.PutWithOneParam
import kalix.spring.testmodels.action.ActionsTestModels.PutWithoutParam
import kalix.spring.testmodels.action.ActionsTestModels.StreamInAction
import kalix.spring.testmodels.action.ActionsTestModels.StreamInOutAction
import kalix.spring.testmodels.action.ActionsTestModels.StreamOutAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.ActionWithMethodLevelAcl
import kalix.spring.testmodels.subscriptions.PubSubTestModels.ActionWithMethodLevelAclAndSubscription
import kalix.spring.testmodels.subscriptions.PubSubTestModels.ActionWithServiceLevelAcl
import kalix.spring.testmodels.subscriptions.PubSubTestModels.AmbiguousDeleteHandlersVESubscriptionInAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.AmbiguousHandlersESSubscriptionInAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.AmbiguousHandlersESTypeLevelSubscriptionInAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.AmbiguousHandlersStreamTypeLevelSubscriptionInAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.AmbiguousHandlersTopiSubscriptionInAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.AmbiguousHandlersTopicTypeLevelSubscriptionInAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.AmbiguousHandlersVESubscriptionInAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.AmbiguousHandlersVETypeLevelSubscriptionInAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.DifferentTopicForESSubscription
import kalix.spring.testmodels.subscriptions.PubSubTestModels.DifferentTopicForESTypeLevelSubscription
import kalix.spring.testmodels.subscriptions.PubSubTestModels.DifferentTopicForStreamSubscription
import kalix.spring.testmodels.subscriptions.PubSubTestModels.DifferentTopicForTopicSubscription
import kalix.spring.testmodels.subscriptions.PubSubTestModels.DifferentTopicForTopicTypeLevelSubscription
import kalix.spring.testmodels.subscriptions.PubSubTestModels.DifferentTopicForVESubscription
import kalix.spring.testmodels.subscriptions.PubSubTestModels.ESWithPublishToTopicAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.EventStreamPublishingAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.EventStreamSubscriptionAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.InvalidSubscribeToEventSourcedEntityAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.InvalidSubscribeToTopicAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.MissingHandlersWhenSubscribeToEventSourcedEntityAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.MissingHandlersWhenSubscribeToEventSourcedOnMethodLevelEntityAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.MissingSourceForTopicPublishing
import kalix.spring.testmodels.subscriptions.PubSubTestModels.MissingTopicForESSubscription
import kalix.spring.testmodels.subscriptions.PubSubTestModels.MissingTopicForStreamSubscription
import kalix.spring.testmodels.subscriptions.PubSubTestModels.MissingTopicForTopicSubscription
import kalix.spring.testmodels.subscriptions.PubSubTestModels.MissingTopicForTopicTypeLevelSubscription
import kalix.spring.testmodels.subscriptions.PubSubTestModels.MissingTopicForTypeLevelESSubscription
import kalix.spring.testmodels.subscriptions.PubSubTestModels.MissingTopicForVESubscription
import kalix.spring.testmodels.subscriptions.PubSubTestModels.RestAnnotatedSubscribeToEventSourcedEntityAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.RestAnnotatedSubscribeToValueEntityAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.RestWithPublishToTopicAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.StreamSubscriptionWithPublishToTopicAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.SubscribeOnlyOneToEventSourcedEntityActionTypeLevel
import kalix.spring.testmodels.subscriptions.PubSubTestModels.SubscribeToEventSourcedEntityAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.SubscribeToTopicAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.SubscribeToTwoTopicsAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.SubscribeToValueEntityAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.SubscribeToValueEntityWithDeletesAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.TypeLevelESWithPublishToTopicAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.TypeLevelTopicSubscriptionWithPublishToTopicAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.VEWithPublishToTopicAction
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
        assertFieldIsProto3Optional(method, "one");
      }
    }

    "fail to generate mappings for an Action with GET and one optional path param" in {
      intercept[ServiceIntrospectionException] {
        descriptorFor[GetWithOneOptionalPathParam]
      }.getMessage should include("Currently all @PathVariables must be defined as required.")
    }

    "generate mappings for an Action with GET and one query param" in {
      assertDescriptor[GetWithOneQueryParam] { desc =>

        val methodDescriptor = desc.serviceDescriptor.findMethodByName("Message")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false

        val method = desc.commandHandlers("Message")
        assertRequestFieldJavaType(method, "one", JavaType.STRING)
        assertFieldIsProto3Optional(method, "one")
      }
    }

    "generate mappings for an Action with GET and one path variable and query param" in {
      assertDescriptor[GetWithOnePathVariableAndQueryParam] { desc =>

        val methodDescriptor = desc.serviceDescriptor.findMethodByName("Message")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false

        val method = desc.commandHandlers("Message")
        assertRequestFieldJavaType(method, "one", JavaType.STRING)
        assertFieldIsProto3Optional(method, "one")

        assertRequestFieldJavaType(method, "two", JavaType.STRING)
        assertFieldIsProto3Optional(method, "two")
      }
    }

    "generate mappings for an Action with GET and one optional query param" in {
      assertDescriptor[GetWithOneOptionalQueryParam] { desc =>
        val method = desc.commandHandlers("Message")
        assertRequestFieldRequested(method, "one", isRequired = false)
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
        val methodDescriptor = findMethodByName(desc, "KalixSyntheticMethodOnESCounterentity")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false

        val methodOne = desc.commandHandlers("KalixSyntheticMethodOnESCounterentity")
        methodOne.requestMessageDescriptor.getFullName shouldBe JavaPbAny.getDescriptor.getFullName

        val eventSourceOne = findKalixMethodOptions(methodDescriptor).getEventing.getIn
        eventSourceOne.getEventSourcedEntity shouldBe "counter-entity"
      }
    }

    "fail if has both Event Sourced Entity Subscription and REST annotations" in {
      intercept[InvalidComponentException] {
        Validations
          .validate(classOf[RestAnnotatedSubscribeToEventSourcedEntityAction])
          .failIfInvalid
      }.getMessage should include(
        "Methods annotated with Kalix @Subscription annotations are for internal use only and cannot be annotated with REST annotations.")
    }

    "generate mapping with Value Entity Subscription annotations" in {
      assertDescriptor[SubscribeToValueEntityAction] { desc =>

        val onUpdateMethodDescriptor = findMethodByName(desc, "OnUpdate")
        onUpdateMethodDescriptor.isServerStreaming shouldBe false
        onUpdateMethodDescriptor.isClientStreaming shouldBe false

        val onUpdateMethod = desc.commandHandlers("OnUpdate")
        onUpdateMethod.requestMessageDescriptor.getFullName shouldBe JavaPbAny.getDescriptor.getFullName

        val eventing = findKalixMethodOptions(onUpdateMethodDescriptor).getEventing.getIn
        eventing.getValueEntity shouldBe "ve-counter"

        // should have a default extractor for any payload
        val javaMethod = onUpdateMethod.methodInvokers.values.head
        javaMethod.parameterExtractors.length shouldBe 1
      }
    }

    "generate mapping with Value Entity and delete handler" in {
      assertDescriptor[SubscribeToValueEntityWithDeletesAction] { desc =>

        val onUpdateMethodDescriptor = findMethodByName(desc, "OnUpdate")
        onUpdateMethodDescriptor.isServerStreaming shouldBe false
        onUpdateMethodDescriptor.isClientStreaming shouldBe false

        val onUpdateMethod = desc.commandHandlers("OnUpdate")
        onUpdateMethod.requestMessageDescriptor.getFullName shouldBe JavaPbAny.getDescriptor.getFullName

        val eventing = findKalixMethodOptions(onUpdateMethodDescriptor).getEventing.getIn
        eventing.getValueEntity shouldBe "ve-counter"
        eventing.getHandleDeletes shouldBe false

        val onDeleteMethodDescriptor = findMethodByName(desc, "OnDelete")
        onDeleteMethodDescriptor.isServerStreaming shouldBe false
        onDeleteMethodDescriptor.isClientStreaming shouldBe false

        val onDeleteMethod = desc.commandHandlers("OnDelete")
        onDeleteMethod.requestMessageDescriptor.getFullName shouldBe Empty.javaDescriptor.getFullName

        val deleteEventing = findKalixMethodOptions(onDeleteMethodDescriptor).getEventing.getIn
        deleteEventing.getValueEntity shouldBe "ve-counter"
        deleteEventing.getHandleDeletes shouldBe true
      }
    }

    "fail if has both Value Entity Subscription and REST annotations" in {
      intercept[InvalidComponentException] {
        Validations
          .validate(classOf[RestAnnotatedSubscribeToValueEntityAction])
          .failIfInvalid
      }.getMessage should include(
        "Methods annotated with Kalix @Subscription annotations are for internal use only and cannot be annotated with REST annotations.")
    }

    "generate stream out methods" in {
      assertDescriptor[StreamOutAction] { desc =>
        val methodDescriptor = desc.serviceDescriptor.findMethodByName("Message")
        methodDescriptor.isServerStreaming shouldBe true
        methodDescriptor.isClientStreaming shouldBe false
      }
    }

    "generate stream in methods" in {
      intercept[InvalidComponentException] {
        Validations
          .validate(classOf[StreamInAction])
          .failIfInvalid
      }.getMessage should include("Stream in calls are not supported.")
    }

    "generate stream in/out methods" in {
      intercept[InvalidComponentException] {
        Validations
          .validate(classOf[StreamInOutAction])
          .failIfInvalid
      }.getMessage should include("Stream in calls are not supported.")
    }

    "generate mapping for an Action with a subscription to a topic" in {
      assertDescriptor[SubscribeToTopicAction] { desc =>
        val methodOne = desc.commandHandlers("MessageOne")
        methodOne.requestMessageDescriptor.getFullName shouldBe JavaPbAny.getDescriptor.getFullName

        val eventSourceOne = findKalixMethodOptions(desc, "MessageOne").getEventing.getIn
        eventSourceOne.getTopic shouldBe "topicXYZ"
        eventSourceOne.getConsumerGroup shouldBe "cg"

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
        eventSourceOne.getEventSourcedEntity shouldBe "counter-entity"
        // we don't set the property so the proxy won't ignore. Ignore is only internal to the SDK
        eventSourceOne.getIgnore shouldBe false
        eventSourceOne.getIgnoreUnknown shouldBe false
      }
    }

    "validates it is forbidden Entity Subscription at annotation type level and method level at the same time" in {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[InvalidSubscribeToEventSourcedEntityAction]).failIfInvalid
      }.getMessage should include("You cannot use @Subscribe.EventSourcedEntity annotation in both methods and class.")
    }

    "validates that ambiguous handler VE" in {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[AmbiguousHandlersVESubscriptionInAction]).failIfInvalid
      }.getMessage should include(
        "Ambiguous handlers for java.lang.Integer, methods: [methodOne, methodTwo] consume the same type.")
    }

    "validates that ambiguous delete handler VE" in {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[AmbiguousDeleteHandlersVESubscriptionInAction]).failIfInvalid
      }.getMessage should include("Ambiguous delete handlers: [methodOne, methodTwo].")
    }

    "validates that ambiguous handler VE (type level)" in {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[AmbiguousHandlersVETypeLevelSubscriptionInAction]).failIfInvalid
      }.getMessage should include(
        "Ambiguous handlers for java.lang.Integer, methods: [methodOne, methodTwo] consume the same type.")
    }

    "validates that ambiguous handler ES" in {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[AmbiguousHandlersESSubscriptionInAction]).failIfInvalid
      }.getMessage should include(
        "Ambiguous handlers for java.lang.Integer, methods: [methodOne, methodTwo] consume the same type.")
    }

    "validates that ambiguous handler ES (type level)" in {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[AmbiguousHandlersESTypeLevelSubscriptionInAction]).failIfInvalid
      }.getMessage should include(
        "Ambiguous handlers for java.lang.Integer, methods: [methodOne, methodTwo] consume the same type.")
    }

    "validates that ambiguous handler Stream (type level)" in {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[AmbiguousHandlersStreamTypeLevelSubscriptionInAction]).failIfInvalid
      }.getMessage should include(
        "Ambiguous handlers for java.lang.Integer, methods: [methodOne, methodTwo] consume the same type.")
    }

    "validates that ambiguous handler Topic" in {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[AmbiguousHandlersTopiSubscriptionInAction]).failIfInvalid
      }.getMessage should include(
        "Ambiguous handlers for java.lang.Integer, methods: [methodOne, methodTwo] consume the same type.")
    }

    "validates that ambiguous handler Topic (type level)" in {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[AmbiguousHandlersTopicTypeLevelSubscriptionInAction]).failIfInvalid
      }.getMessage should include(
        "Ambiguous handlers for java.lang.Integer, methods: [methodOne, methodTwo] consume the same type.")
    }

    "validates that source is missing for topic publication" in {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[MissingSourceForTopicPublishing]).failIfInvalid
      }.getMessage should include(
        "You must select a source for @Publish.Topic. Annotate this methods with one of @Subscribe or REST annotations.")
    }

    "validates that topic is missing for VE subscription" in {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[MissingTopicForVESubscription]).failIfInvalid
      }.getMessage should include(
        "Add @Publish.Topic annotation to all subscription methods from ValueEntity \"ve-counter\". Or remove it from all methods.")
    }

    "validates that topic is missing for ES subscription" in {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[MissingTopicForESSubscription]).failIfInvalid
      }.getMessage should include(
        "On 'kalix.spring.testmodels.subscriptions.PubSubTestModels$MissingTopicForESSubscription': Add @Publish.Topic annotation to all subscription methods from EventSourcedEntity \"employee\". Or remove it from all methods.")
    }

    "validates that topic is missing for ES subscription (type level)" in {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[MissingTopicForTypeLevelESSubscription]).failIfInvalid
      }.getMessage should include(
        "On 'kalix.spring.testmodels.subscriptions.PubSubTestModels$MissingTopicForTypeLevelESSubscription': Add @Publish.Topic annotation to all subscription methods from EventSourcedEntity \"employee\". Or remove it from all methods.")
    }

    "validates that topic is missing for Topic subscription" in {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[MissingTopicForTopicSubscription]).failIfInvalid
      }.getMessage should include(
        "On 'kalix.spring.testmodels.subscriptions.PubSubTestModels$MissingTopicForTopicSubscription': Add @Publish.Topic annotation to all subscription methods from Topic \"source\". Or remove it from all methods.")
    }

    "validates that topic is missing for Topic subscription (type level)" in {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[MissingTopicForTopicTypeLevelSubscription]).failIfInvalid
      }.getMessage should include(
        "On 'kalix.spring.testmodels.subscriptions.PubSubTestModels$MissingTopicForTopicTypeLevelSubscription': Add @Publish.Topic annotation to all subscription methods from Topic \"source\". Or remove it from all methods.")
    }

    "validates that topic is missing for Stream subscription" in {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[MissingTopicForStreamSubscription]).failIfInvalid
      }.getMessage should include(
        "On 'kalix.spring.testmodels.subscriptions.PubSubTestModels$MissingTopicForStreamSubscription': Add @Publish.Topic annotation to all subscription methods from Stream \"source\". Or remove it from all methods.")
    }

    "validates that topic names are the same for VE subscription" in {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[DifferentTopicForVESubscription]).failIfInvalid
      }.getMessage should include(
        "On 'kalix.spring.testmodels.subscriptions.PubSubTestModels$DifferentTopicForVESubscription': All @Publish.Topic annotation for the same subscription source ValueEntity \"ve-counter\" should point to the same topic name. Create a separate Action if you want to split messages to different topics from the same source.")
    }

    "validates that topic names are the same for ES subscription" in {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[DifferentTopicForESSubscription]).failIfInvalid
      }.getMessage should include(
        "On 'kalix.spring.testmodels.subscriptions.PubSubTestModels$DifferentTopicForESSubscription': All @Publish.Topic annotation for the same subscription source EventSourcedEntity \"employee\" should point to the same topic name. Create a separate Action if you want to split messages to different topics from the same source.")
    }

    "validates that topic names are the same for ES subscription (type level)" in {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[DifferentTopicForESTypeLevelSubscription]).failIfInvalid
      }.getMessage should include(
        "On 'kalix.spring.testmodels.subscriptions.PubSubTestModels$DifferentTopicForESTypeLevelSubscription': All @Publish.Topic annotation for the same subscription source EventSourcedEntity \"employee\" should point to the same topic name. Create a separate Action if you want to split messages to different topics from the same source.")
    }

    "validates that topic names are the same for Topic subscription" in {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[DifferentTopicForTopicSubscription]).failIfInvalid
      }.getMessage should include(
        "On 'kalix.spring.testmodels.subscriptions.PubSubTestModels$DifferentTopicForTopicSubscription': All @Publish.Topic annotation for the same subscription source Topic \"source\" should point to the same topic name. Create a separate Action if you want to split messages to different topics from the same source.")
    }

    "validates that topic names are the same for Topic subscription (type level)" in {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[DifferentTopicForTopicTypeLevelSubscription]).failIfInvalid
      }.getMessage should include(
        "On 'kalix.spring.testmodels.subscriptions.PubSubTestModels$DifferentTopicForTopicTypeLevelSubscription': All @Publish.Topic annotation for the same subscription source Topic \"source\" should point to the same topic name. Create a separate Action if you want to split messages to different topics from the same source.")
    }

    "validates that topic names are the same for Stream subscription" in {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[DifferentTopicForStreamSubscription]).failIfInvalid
      }.getMessage should include(
        "On 'kalix.spring.testmodels.subscriptions.PubSubTestModels$DifferentTopicForStreamSubscription': All @Publish.Topic annotation for the same subscription source Stream \"source\" should point to the same topic name. Create a separate Action if you want to split messages to different topics from the same source.")
    }

    //TODO remove ignore after updating to Scala 2.13.11 (https://github.com/scala/scala/pull/10105)
    "validates if there are missing event handlers for event sourced Entity Subscription at type level" ignore {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[MissingHandlersWhenSubscribeToEventSourcedEntityAction]).failIfInvalid
      }.getMessage should include(
        "Component 'MissingHandlersWhenSubscribeToEventSourcedEntityAction' is missing an event handler for 'kalix.spring.testmodels.eventsourcedentity.EmployeeEvent$EmployeeEmailUpdated'")
    }

    //TODO remove ignore after updating to Scala 2.13.11 (https://github.com/scala/scala/pull/10105)
    "validates if there are missing event handlers for event sourced Entity Subscription at method level" ignore {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[MissingHandlersWhenSubscribeToEventSourcedOnMethodLevelEntityAction]).failIfInvalid
      }.getMessage should include(
        "Component 'MissingHandlersWhenSubscribeToEventSourcedOnMethodLevelEntityAction' is missing an event handler for 'kalix.spring.testmodels.eventsourcedentity.EmployeeEvent$EmployeeEmailUpdated'")
    }

    "validates it is forbidden Topic Subscription at annotation type level and method level at the same time" in {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[InvalidSubscribeToTopicAction]).failIfInvalid
      }.getMessage should include("You cannot use @Subscribe.Topic annotation in both methods and class.")
    }

    "generate mapping for an Action with a Rest endpoint and publication to a topic" in {
      assertDescriptor[RestWithPublishToTopicAction] { desc =>
        val methodOne = desc.commandHandlers("MessageOne")
        methodOne.requestMessageDescriptor.getFullName shouldBe "kalix.spring.testmodels.subscriptions.MessageOneKalixSyntheticRequest"

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

    "generate mapping for an Action with a VE subscription and publication to a topic" in {
      assertDescriptor[VEWithPublishToTopicAction] { desc =>
        val methodOne = desc.commandHandlers("MessageOne")
        methodOne.requestMessageDescriptor.getFullName shouldBe JavaPbAny.getDescriptor.getFullName

        val eventDestinationOne = findKalixMethodOptions(desc, "MessageOne").getEventing.getOut
        eventDestinationOne.getTopic shouldBe "foobar"

        // should have a default extractor for any payload
        val javaMethodOne = methodOne.methodInvokers.values.head
        javaMethodOne.parameterExtractors.length shouldBe 1

        val methodTwo = desc.commandHandlers("MessageTwo")
        methodTwo.requestMessageDescriptor.getFullName shouldBe Empty.javaDescriptor.getFullName

        val eventDestinationTwo = findKalixMethodOptions(desc, "MessageTwo").getEventing.getOut
        eventDestinationTwo.getTopic shouldBe "foobar"

        // delete handler with 0 params
        val javaMethodTwo = methodTwo.methodInvokers.values.head
        javaMethodTwo.parameterExtractors.length shouldBe 0
      }
    }

    "generate mapping for an Action with a ES subscription and publication to a topic" in {
      assertDescriptor[ESWithPublishToTopicAction] { desc =>
        desc.commandHandlers should have size 1

        val methodOne = desc.commandHandlers("KalixSyntheticMethodOnESEmployee")
        methodOne.requestMessageDescriptor.getFullName shouldBe JavaPbAny.getDescriptor.getFullName

        val eventDestinationOne = findKalixMethodOptions(desc, "KalixSyntheticMethodOnESEmployee").getEventing.getOut
        eventDestinationOne.getTopic shouldBe "foobar"
      }
    }

    "generate mapping for an Action with a ES type level subscription and publication to a topic" in {
      assertDescriptor[TypeLevelESWithPublishToTopicAction] { desc =>
        desc.commandHandlers should have size 1

        val methodOne = desc.commandHandlers("KalixSyntheticMethodOnESEmployee")
        methodOne.requestMessageDescriptor.getFullName shouldBe JavaPbAny.getDescriptor.getFullName

        val eventDestinationOne = findKalixMethodOptions(desc, "KalixSyntheticMethodOnESEmployee").getEventing.getOut
        eventDestinationOne.getTopic shouldBe "foobar"
      }
    }

    "generate mapping for an Action with a Topic subscription and publication to a topic" in {
      assertDescriptor[TypeLevelTopicSubscriptionWithPublishToTopicAction] { desc =>
        desc.commandHandlers should have size 1

        val methodOne = desc.commandHandlers("KalixSyntheticMethodOnTopicSource")
        methodOne.requestMessageDescriptor.getFullName shouldBe JavaPbAny.getDescriptor.getFullName

        val eventDestinationOne = findKalixMethodOptions(desc, "KalixSyntheticMethodOnTopicSource").getEventing.getOut
        eventDestinationOne.getTopic shouldBe "foobar"
      }
    }

    "generate mapping for an Action with a Topic type level subscription and publication to a topic" in {
      assertDescriptor[TypeLevelTopicSubscriptionWithPublishToTopicAction] { desc =>
        desc.commandHandlers should have size 1

        val methodOne = desc.commandHandlers("KalixSyntheticMethodOnTopicSource")
        methodOne.requestMessageDescriptor.getFullName shouldBe JavaPbAny.getDescriptor.getFullName

        val eventDestinationOne = findKalixMethodOptions(desc, "KalixSyntheticMethodOnTopicSource").getEventing.getOut
        eventDestinationOne.getTopic shouldBe "foobar"
      }
    }

    "generate mapping for an Action with a Stream subscription and publication to a topic" in {
      assertDescriptor[StreamSubscriptionWithPublishToTopicAction] { desc =>
        desc.commandHandlers should have size 1

        val methodOne = desc.commandHandlers("KalixSyntheticMethodOnESSource")
        methodOne.requestMessageDescriptor.getFullName shouldBe JavaPbAny.getDescriptor.getFullName

        val eventDestinationOne = findKalixMethodOptions(desc, "KalixSyntheticMethodOnESSource").getEventing.getOut
        eventDestinationOne.getTopic shouldBe "foobar"
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
      intercept[InvalidComponentException] {
        Validations.validate(classOf[ActionWithMethodLevelAclAndSubscription]).failIfInvalid
      }.getMessage should include(
        "Methods annotated with Kalix @Subscription annotations are for internal use only and cannot be annotated with ACL annotations.")
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

        val methodDescriptor = findMethodByName(desc, "KalixSyntheticMethodOnESEmployeeevents")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false
      }
    }
  }

}
