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

import com.google.protobuf.Any
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType
import kalix.KeyGeneratorMethodOptions
import kalix.spring.testmodels.valueentity.Counter
import kalix.spring.testmodels.valueentity.ValueEntitiesTestModels.GetWithQueryParams
import kalix.spring.testmodels.valueentity.ValueEntitiesTestModels.PostWithIds
import kalix.spring.testmodels.valueentity.ValueEntitiesTestModels.ValueEntityWithMethodLevelAcl
import kalix.spring.testmodels.valueentity.ValueEntitiesTestModels.ValueEntityWithServiceLevelAcl
import org.scalatest.wordspec.AnyWordSpec

class ValueEntityDescriptorFactorySpec extends AnyWordSpec with ComponentDescriptorSuite {

  "ValueEntity descriptor factory" should {
    "generate mappings for a Value Entity with entity ids in path" in {
      assertDescriptor[PostWithIds] { desc =>
        val method = desc.commandHandlers("CreateEntity")
        assertRequestFieldJavaType(method, "json_body", JavaType.MESSAGE)
        assertRequestFieldMessageType(method, "json_body", Any.getDescriptor.getFullName)

        assertRequestFieldJavaType(method, "userId", JavaType.STRING)
        assertEntityKeyField(method, "userId")

        assertRequestFieldJavaType(method, "cartId", JavaType.STRING)
        assertEntityKeyField(method, "cartId")
      }
    }

    "generate mappings for a Value Entity with generated id" in {
      assertDescriptor[Counter] { desc =>
        val method = desc.commandHandlers("RandomIncrease")
        assertRequestFieldNumberAndJavaType(method, "value", 2, JavaType.INT)

        val extension = findKalixMethodOptions(desc, "RandomIncrease")
        extension.getEntity.getKeyGenerator shouldBe KeyGeneratorMethodOptions.Generator.VERSION_4_UUID
      }
    }

    "generate mappings for a Value Entity query params in path" in {
      assertDescriptor[GetWithQueryParams] { desc =>
        val method = desc.commandHandlers("GetUser")

        assertRequestFieldNumberAndJavaType(method, "userId", 2, JavaType.STRING)
        assertRequestFieldNumberAndJavaType(method, "cartId", 3, JavaType.STRING)
        assertRequestFieldNumberAndJavaType(method, "otherParam", 4, JavaType.INT)
        assertRequestFieldNumberAndJavaType(method, "someParam", 5, JavaType.STRING)

        val createMethod = desc.commandHandlers("CreateEntity2")

        assertRequestFieldNumberAndJavaType(createMethod, "json_body", 1, JavaType.MESSAGE)
        assertRequestFieldNumberAndJavaType(createMethod, "cartId", 2, JavaType.STRING)
        assertRequestFieldNumberAndJavaType(createMethod, "otherParam", 3, JavaType.INT)
        assertRequestFieldNumberAndJavaType(createMethod, "someParam", 4, JavaType.STRING)
        assertRequestFieldNumberAndJavaType(createMethod, "userId", 5, JavaType.STRING)
      }
    }

    "generate ACL annotations at service level" in {
      assertDescriptor[ValueEntityWithServiceLevelAcl] { desc =>
        val extension = desc.serviceDescriptor.getOptions.getExtension(kalix.Annotations.service)
        val service = extension.getAcl.getAllow(0).getService
        service shouldBe "test"
      }
    }

    "generate ACL annotations at method level" in {
      assertDescriptor[ValueEntityWithMethodLevelAcl] { desc =>
        val extension = findKalixMethodOptions(desc, "CreateEntity")
        val service = extension.getAcl.getAllow(0).getService
        service shouldBe "test"
      }
    }
  }

}
