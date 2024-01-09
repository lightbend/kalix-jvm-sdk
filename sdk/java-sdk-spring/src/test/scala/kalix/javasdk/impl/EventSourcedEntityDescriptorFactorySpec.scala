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
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.EventSourcedEntityWithMethodLevelAcl
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.EventSourcedEntityWithServiceLevelAcl
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.IllDefinedEntityWithIdGeneratorAndId
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.IllDefinedEntityWithoutIdGeneratorNorId
import org.scalatest.wordspec.AnyWordSpec

import scala.jdk.CollectionConverters.CollectionHasAsScala

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
        assertEntityKeyField(method, "id")
        assertRequestFieldJavaType(method, "number", JavaType.INT)

        val postMethod = desc.commandHandlers("ChangeInteger")
        assertRequestFieldJavaType(postMethod, "id", JavaType.STRING)
        assertEntityKeyField(postMethod, "id")
        assertRequestFieldJavaType(postMethod, "number", JavaType.INT)
      }
    }

    "generate mappings for a Event Sourced with entity ids in path and EntityKey on method" in {
      assertDescriptor[CounterEventSourcedEntityWithIdOnMethod] { desc =>
        val method = desc.commandHandlers("GetInteger")
        assertRequestFieldJavaType(method, "id", JavaType.STRING)
        assertEntityKeyField(method, "id")
        assertRequestFieldJavaType(method, "number", JavaType.INT)
      }
    }

    "generate mappings for a Event Sourced with Id on method overrides EntityKey on type" in {
      assertDescriptor[CounterEventSourcedEntityWithIdMethodOverride] { desc =>
        val method = desc.commandHandlers("GetInteger")
        assertRequestFieldJavaType(method, "counter_id", JavaType.STRING)
        assertEntityKeyField(method, "counter_id")
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

        val keyGenerator = findKalixMethodOptions(desc, method.grpcMethodName).getEntity.getKeyGenerator
        keyGenerator shouldBe Generator.VERSION_4_UUID
      }
    }

    "generate mappings for a Event Sourced with entity ids in path and method level JWT annotation" in {
      assertDescriptor[CounterEventSourcedEntityWithMethodLevelJWT] { desc =>
        val method = desc.commandHandlers("GetInteger")
        assertRequestFieldJavaType(method, "id", JavaType.STRING)
        assertEntityKeyField(method, "id")
        assertRequestFieldJavaType(method, "number", JavaType.INT)

        val jwtOption = findKalixMethodOptions(desc, method.grpcMethodName).getJwt
        jwtOption.getBearerTokenIssuer(0) shouldBe "a"
        jwtOption.getBearerTokenIssuer(1) shouldBe "b"
        jwtOption.getValidate(0) shouldBe JwtMethodMode.BEARER_TOKEN

        val postMethod = desc.commandHandlers("ChangeInteger")
        assertRequestFieldJavaType(postMethod, "id", JavaType.STRING)
        assertEntityKeyField(postMethod, "id")
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
  }

}
