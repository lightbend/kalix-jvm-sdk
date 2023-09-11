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
import kalix.KeyGeneratorMethodOptions.Generator
import kalix.javasdk.impl.reflection.ServiceIntrospectionException
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.CounterEventSourcedEntity
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.CounterEventSourcedEntityWithIdGenerator
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.CounterEventSourcedEntityWithIdMethodOverride
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.CounterEventSourcedEntityWithIdOnMethod
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.CounterEventSourcedEntityWithJWT
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.EventSourcedEntityWithMethodLevelAcl
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.EventSourcedEntityWithServiceLevelAcl
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.IllDefinedEntityWithIdGeneratorAndId
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.IllDefinedEntityWithoutIdGeneratorNorId
import org.scalatest.wordspec.AnyWordSpec

class EventSourcedEntityDescriptorFactorySpec extends AnyWordSpec with ComponentDescriptorSuite {

  "EventSourced descriptor factory" should {
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

    "generate mappings for a Event Sourced with entity ids in path and JWT annotations" in {
      assertDescriptor[CounterEventSourcedEntityWithJWT] { desc =>
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
        jwtOption2.getBearerTokenIssuer(0) shouldBe "a"
        jwtOption2.getBearerTokenIssuer(1) shouldBe "b"
        jwtOption2.getValidate(0) shouldBe JwtMethodMode.BEARER_TOKEN
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
  }

}