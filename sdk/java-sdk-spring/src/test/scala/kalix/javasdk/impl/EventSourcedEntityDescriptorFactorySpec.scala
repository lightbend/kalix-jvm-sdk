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

package kalix.javasdk.impl

import scala.jdk.CollectionConverters.CollectionHasAsScala

import com.google.protobuf.Descriptors.FieldDescriptor.JavaType
import kalix.JwtMethodOptions.JwtMethodMode
import kalix.JwtServiceOptions.JwtServiceMode
import kalix.KeyGeneratorMethodOptions.Generator
import kalix.javasdk.impl.reflection.ServiceIntrospectionException
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.CounterEventSourcedEntity
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.CounterEventSourcedEntityWithIdGenerator
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.CounterEventSourcedEntityWithIdMethodOverride
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.CounterEventSourcedEntityWithIdOnMethod
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.CounterEventSourcedEntityWithMethodLevelJWT
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.CounterEventSourcedEntityWithServiceLevelJWT
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.ESEntityCompoundIdIncorrectOrder
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.EmployeeEntityWithMissingHandler
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.EmployeeEntityWithMixedHandlers
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.ErrorDuplicatedEventsEntity
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.ErrorWrongSignaturesEntity
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.EventSourcedEntityWithMethodLevelAcl
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.EventSourcedEntityWithServiceLevelAcl
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.IllDefinedEntityWithIdGeneratorAndId
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.IllDefinedEntityWithoutIdGeneratorNorId
import org.scalatest.wordspec.AnyWordSpec

class EventSourcedEntityDescriptorFactorySpec extends AnyWordSpec with ComponentDescriptorSuite {

  "EventSourced descriptor factory" should {

    "validate an ESE must be declared as public" in {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[NotPublicComponents.NotPublicEventSourced]).failIfInvalid
      }.getMessage should include(
        "NotPublicEventSourced is not marked with `public` modifier. Components must be public.")
    }

    "generate mappings for a Event Sourced with entity ids in path" in {
      assertDescriptor[CounterEventSourcedEntity] { desc =>
        val method = desc.commandHandlers("GetInteger")
        assertRequestFieldJavaType(method, "id", JavaType.STRING)
        assertEntityIdField(method, "id")
        assertRequestFieldJavaType(method, "number", JavaType.INT)

        val postMethod = desc.commandHandlers("ChangeInteger")
        assertRequestFieldJavaType(postMethod, "id", JavaType.STRING)
        assertEntityIdField(postMethod, "id")
        assertRequestFieldJavaType(postMethod, "number", JavaType.INT)
      }
    }

    "generate mappings for a Event Sourced with entity ids in path and EntityKey on method" in {
      assertDescriptor[CounterEventSourcedEntityWithIdOnMethod] { desc =>
        val method = desc.commandHandlers("GetInteger")
        assertRequestFieldJavaType(method, "id", JavaType.STRING)
        assertEntityIdField(method, "id")
        assertRequestFieldJavaType(method, "number", JavaType.INT)
      }
    }

    "generate mappings for a Event Sourced with Id on method overrides EntityKey on type" in {
      assertDescriptor[CounterEventSourcedEntityWithIdMethodOverride] { desc =>
        val method = desc.commandHandlers("GetInteger")
        assertRequestFieldJavaType(method, "counter_id", JavaType.STRING)
        assertEntityIdField(method, "counter_id")
        assertRequestFieldJavaType(method, "number", JavaType.INT)
      }
    }

    "fail if mix EntityKey and GenerateId on method" in {
      intercept[ServiceIntrospectionException] {
        descriptorFor[IllDefinedEntityWithIdGeneratorAndId]
      }.getMessage should include("Invalid annotation usage. Found both @Id and @GenerateId annotations.")
    }

    "fail if no EntityKey nor GenerateId is defined" in {
      intercept[ServiceIntrospectionException] {
        descriptorFor[IllDefinedEntityWithoutIdGeneratorNorId]
      }.getMessage should include("Invalid command method. No @Id nor @GenerateId annotations found.")
    }

    "generate mappings for a Event Sourced with GenerateId" in {
      assertDescriptor[CounterEventSourcedEntityWithIdGenerator] { desc =>
        val method = desc.commandHandlers("GetInteger")
        assertRequestFieldJavaType(method, "number", JavaType.INT)

        val generator = findKalixMethodOptions(desc, method.grpcMethodName).getIdGenerator.getAlgorithm
        generator shouldBe Generator.VERSION_4_UUID
      }
    }

    "generate mappings for a Event Sourced with entity ids in path and method level JWT annotation" in {
      assertDescriptor[CounterEventSourcedEntityWithMethodLevelJWT] { desc =>
        val method = desc.commandHandlers("GetInteger")
        assertRequestFieldJavaType(method, "id", JavaType.STRING)
        assertEntityIdField(method, "id")
        assertRequestFieldJavaType(method, "number", JavaType.INT)

        val jwtOption = findKalixMethodOptions(desc, method.grpcMethodName).getJwt
        jwtOption.getBearerTokenIssuer(0) shouldBe "a"
        jwtOption.getBearerTokenIssuer(1) shouldBe "b"
        jwtOption.getValidate(0) shouldBe JwtMethodMode.BEARER_TOKEN

        val postMethod = desc.commandHandlers("ChangeInteger")
        assertRequestFieldJavaType(postMethod, "id", JavaType.STRING)
        assertEntityIdField(postMethod, "id")
        assertRequestFieldJavaType(postMethod, "number", JavaType.INT)

        val jwtOption2 = findKalixMethodOptions(desc, postMethod.grpcMethodName).getJwt
        jwtOption2.getBearerTokenIssuer(0) shouldBe "c"
        jwtOption2.getBearerTokenIssuer(1) shouldBe "d"
        jwtOption2.getValidate(0) shouldBe JwtMethodMode.BEARER_TOKEN

        val Seq(claim1, claim2) = jwtOption2.getStaticClaimList.asScala.toSeq
        claim1.getClaim shouldBe "role"
        claim1.getValue(0) shouldBe "method-admin"
        claim2.getClaim shouldBe "aud"
        claim2.getValue(0) shouldBe "${ENV}"
      }
    }

    "generate mappings for a Event Sourced with service level JWT annotation" in {
      assertDescriptor[CounterEventSourcedEntityWithServiceLevelJWT] { desc =>
        val extension = desc.serviceDescriptor.getOptions.getExtension(kalix.Annotations.service)
        val jwtOption = extension.getJwt
        jwtOption.getBearerTokenIssuer(0) shouldBe "a"
        jwtOption.getBearerTokenIssuer(1) shouldBe "b"
        jwtOption.getValidate shouldBe JwtServiceMode.BEARER_TOKEN

        val Seq(claim1, claim2) = jwtOption.getStaticClaimList.asScala.toSeq
        claim1.getClaim shouldBe "role"
        claim1.getValue(0) shouldBe "admin"
        claim2.getClaim shouldBe "aud"
        claim2.getValue(0) shouldBe "${ENV}.kalix.io"
      }
    }

    "generate ACL annotations at service level" in {
      assertDescriptor[EventSourcedEntityWithServiceLevelAcl] { desc =>
        val extension = desc.serviceDescriptor.getOptions.getExtension(kalix.Annotations.service)
        val service = extension.getAcl.getAllow(0).getService
        service shouldBe "test"
      }
    }

    "generate ACL annotations at method level" in {
      assertDescriptor[EventSourcedEntityWithMethodLevelAcl] { desc =>
        val extension = findKalixMethodOptions(desc, "CreateUser")
        val service = extension.getAcl.getAllow(0).getService
        service shouldBe "test"
      }
    }

    "not allow different order of entity ids in the path" in {
      // it should be annotated either on type or on method level
      intercept[InvalidComponentException] {
        Validations.validate(classOf[ESEntityCompoundIdIncorrectOrder]).failIfInvalid
      }.getMessage should include(
        "Ids in the path '/{id2}/eventsourced/{id}/int/{number}' are in a different order than specified in the @Id annotation [id, id2]. This could lead to unexpected bugs when calling the component.")
    }

    "not allow handlers with duplicates signatures (receiving the same event type)" in {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[ErrorDuplicatedEventsEntity]).failIfInvalid
      }.getMessage shouldBe
      "On 'kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels$ErrorDuplicatedEventsEntity': Ambiguous handlers for java.lang.Integer, methods: [receivedIntegerEvent, receivedIntegerEventDup] consume the same type."

    }

    "report error on missing event handler for sealed event interface" in {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[EmployeeEntityWithMissingHandler]).failIfInvalid
      }.getMessage shouldBe
      "On 'kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels$EmployeeEntityWithMissingHandler': missing an event handler for 'kalix.spring.testmodels.eventsourcedentity.EmployeeEvent$EmployeeEmailUpdated'."
    }

    "report error sealed interface event handler is mixed with specific event handlers" in {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[EmployeeEntityWithMixedHandlers]).failIfInvalid
      }.getMessage shouldBe
      "On 'kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels$EmployeeEntityWithMixedHandlers': Event handler accepting a sealed interface [onEvent] cannot be mixed with handlers for specific events. Please remove following handlers: [onEmployeeCreated]."
    }

    "report error on annotated handlers with wrong return type or number of params" in {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[ErrorWrongSignaturesEntity]).failIfInvalid
      }.getMessage shouldBe
      "On 'kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels$ErrorWrongSignaturesEntity': event handler [receivedIntegerEvent] must be public, with exactly one parameter and return type 'java.lang.Integer'., On 'kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels$ErrorWrongSignaturesEntity': event handler [receivedIntegerEventAndString] must be public, with exactly one parameter and return type 'java.lang.Integer'."
    }
  }

}
