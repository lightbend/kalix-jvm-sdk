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
import kalix.JwtMethodOptions.JwtMethodMode
import kalix.KeyGeneratorMethodOptions.Generator
import kalix.javasdk.impl.reflection.ServiceIntrospectionException
import kalix.springsdk.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.CounterEventSourcedEntity
import kalix.springsdk.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.CounterEventSourcedEntityWithEntityKeyGenerator
import kalix.springsdk.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.CounterEventSourcedEntityWithEntityKeyMethodOverride
import kalix.springsdk.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.CounterEventSourcedEntityWithEntityKeyOnMethod
import kalix.springsdk.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.CounterEventSourcedEntityWithJWT
import kalix.springsdk.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.EventSourcedEntityWithMethodLevelAcl
import kalix.springsdk.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.EventSourcedEntityWithServiceLevelAcl
import kalix.springsdk.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.IllDefinedEntityWithEntityKeyGeneratorAndEntityKey
import kalix.springsdk.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.IllDefinedEntityWithoutEntityKeyGeneratorNorEntityKey
import org.scalatest.wordspec.AnyWordSpec

class EventSourcedEntityDescriptorFactorySpec extends AnyWordSpec with ComponentDescriptorSuite {

  "EventSourced descriptor factory" should {
    "generate mappings for a Event Sourced with entity keys in path" in {
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

    "generate mappings for a Event Sourced with entity keys in path and EntityKey on method" in {
      assertDescriptor[CounterEventSourcedEntityWithEntityKeyOnMethod] { desc =>
        val method = desc.commandHandlers("GetInteger")
        assertRequestFieldJavaType(method, "id", JavaType.STRING)
        assertEntityKeyField(method, "id")
        assertRequestFieldJavaType(method, "number", JavaType.INT)
      }
    }

    "generate mappings for a Event Sourced with EntityKey on method overrides EntityKey on type" in {
      assertDescriptor[CounterEventSourcedEntityWithEntityKeyMethodOverride] { desc =>
        val method = desc.commandHandlers("GetInteger")
        assertRequestFieldJavaType(method, "counter_id", JavaType.STRING)
        assertEntityKeyField(method, "counter_id")
        assertRequestFieldJavaType(method, "number", JavaType.INT)
      }
    }

    "fail if mix EntityKey and GenerateEntityKey on method" in {
      intercept[ServiceIntrospectionException] {
        descriptorFor[IllDefinedEntityWithEntityKeyGeneratorAndEntityKey]
      }.getMessage should include("Invalid annotation usage. Found both @EntityKey and @GenerateEntityKey annotations.")
    }

    "fail if no EntityKey nor GenerateEntityKey is defined" in {
      intercept[ServiceIntrospectionException] {
        descriptorFor[IllDefinedEntityWithoutEntityKeyGeneratorNorEntityKey]
      }.getMessage should include("Invalid command method. No @EntityKey nor @GenerateEntityKey annotations found.")
    }

    "generate mappings for a Event Sourced with GenerateEntityKey" in {
      assertDescriptor[CounterEventSourcedEntityWithEntityKeyGenerator] { desc =>
        val method = desc.commandHandlers("GetInteger")
        assertRequestFieldJavaType(method, "number", JavaType.INT)

        val keyGenerator = findKalixMethodOptions(desc, method.grpcMethodName).getEntity.getKeyGenerator
        keyGenerator shouldBe Generator.VERSION_4_UUID
      }
    }

    "generate mappings for a Event Sourced with entity keys in path and JWT annotations" in {
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
