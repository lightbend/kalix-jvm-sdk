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
import kalix.spring.testmodels.action.ActionsTestModels.DeleteWithOneParam
import kalix.spring.testmodels.action.ActionsTestModels.GetClassLevel
import kalix.spring.testmodels.action.ActionsTestModels.GetWithOneOptionalPathParam
import kalix.spring.testmodels.action.ActionsTestModels.GetWithOneOptionalQueryParam
import kalix.spring.testmodels.action.ActionsTestModels.GetWithOneParam
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
import kalix.spring.testmodels.subscriptions.PubSubTestModels.EventStreamPublishingAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.EventStreamSubscriptionAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.InvalidSubscribeToEventSourcedEntityAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.InvalidSubscribeToTopicAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.MissingHandlersWhenSubscribeToEventSourcedEntityAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.MissingHandlersWhenSubscribeToEventSourcedOnMethodLevelEntityAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.PublishToTopicAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.RestAnnotatedSubscribeToEventSourcedEntityAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.RestAnnotatedSubscribeToValueEntityAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.RestWithPublishToTopicAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.SubscribeOnlyOneToEventSourcedEntityActionTypeLevel
import kalix.spring.testmodels.subscriptions.PubSubTestModels.SubscribeToEventSourcedEntityAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.SubscribeToTopicAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.SubscribeToTwoTopicsAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.SubscribeToValueEntityAction
import kalix.spring.testmodels.subscriptions.PubSubTestModels.SubscribeToValueEntityWithDeletesAction
import org.scalatest.Ignore
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
        assertRequestFieldRequested(method, "one", isRequired = true)
      }
    }

    "generate mappings for an Action with GET and one optional path param" in {
      assertDescriptor[GetWithOneOptionalPathParam] { desc =>
        val method = desc.commandHandlers("Message")
        assertRequestFieldRequested(method, "one", isRequired = false)
      }
    }

    "generate mappings for an Action with GET and one query param" in {
      assertDescriptor[GetWithOneQueryParam] { desc =>

        val methodDescriptor = desc.serviceDescriptor.findMethodByName("Message")
        methodDescriptor.isServerStreaming shouldBe false
        methodDescriptor.isClientStreaming shouldBe false

        val method = desc.commandHandlers("Message")
        assertRequestFieldJavaType(method, "one", JavaType.STRING)
        assertRequestFieldRequested(method, "one", isRequired = true)
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

    "generate mapping for an Action with a publication to a topic" in {
      assertDescriptor[PublishToTopicAction] { desc =>
        val methodOne = desc.commandHandlers("MessageOne")
        methodOne.requestMessageDescriptor.getFullName shouldBe JavaPbAny.getDescriptor.getFullName

        val eventDestinationOne = findKalixMethodOptions(desc, "MessageOne").getEventing.getOut
        eventDestinationOne.getTopic shouldBe "topicAlpha"

        // should have a default extractor for any payload
        val javaMethod = methodOne.methodInvokers.values.head
        javaMethod.parameterExtractors.length shouldBe 1
      }

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
