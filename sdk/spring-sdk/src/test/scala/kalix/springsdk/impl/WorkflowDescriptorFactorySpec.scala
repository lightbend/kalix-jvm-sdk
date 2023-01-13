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
import kalix.springsdk.impl.reflection.ServiceIntrospectionException
import kalix.springsdk.testmodels.workflow.WorkflowTestModels
import kalix.springsdk.testmodels.workflow.WorkflowTestModels.WorkflowWithoutIdGeneratorAndEntityKey
import org.scalatest.wordspec.AnyWordSpec

class WorkflowDescriptorFactorySpec extends AnyWordSpec with ComponentDescriptorSuite {

  "Workflow descriptor factory" should {
    "generate mappings for a Workflow with entity keys in path" in {
      assertDescriptor[WorkflowTestModels.WorkflowWithTypeLevelKey] { desc =>
        val method = desc.commandHandlers("StartTransfer")
        val fieldKey = "transferId"
        assertRequestFieldJavaType(method, fieldKey, JavaType.STRING)
        assertEntityKeyField(method, fieldKey)
        assertRequestFieldJavaType(method, "json_body", JavaType.MESSAGE)
      }
    }

    "generate mappings for a Workflow with  keys in path and EntityKey on method" in {
      assertDescriptor[WorkflowTestModels.WorkflowWithMethodLevelKey] { desc =>
        val method = desc.commandHandlers("StartTransfer")
        val fieldKey = "transferId"
        assertRequestFieldJavaType(method, fieldKey, JavaType.STRING)
        assertEntityKeyField(method, fieldKey)
        assertRequestFieldJavaType(method, "json_body", JavaType.MESSAGE)
      }
    }

    "generate mappings for a Workflow with EntityKey on method overrides EntityKey on type" in {
      assertDescriptor[WorkflowTestModels.WorkflowWithKeyOverridden] { desc =>
        val method = desc.commandHandlers("StartTransfer")
        val fieldKey = "transferId"
        assertRequestFieldJavaType(method, fieldKey, JavaType.STRING)
        assertEntityKeyField(method, fieldKey)
        assertRequestFieldJavaType(method, "json_body", JavaType.MESSAGE)
      }
    }

    "fail if mix EntityKey and GenerateEntityKey on method" in {
      intercept[ServiceIntrospectionException] {
        descriptorFor[WorkflowTestModels.WorkflowWithIllDefinedIdGenerator]
      }.getMessage should include("Invalid annotation usage. Found both @EntityKey and @GenerateEntityKey annotations.")
    }

    "fail if no EntityKey nor GenerateEntityKey is defined" in {
      intercept[ServiceIntrospectionException] {
        descriptorFor[WorkflowWithoutIdGeneratorAndEntityKey]
      }.getMessage should include("Invalid command method. No @EntityKey nor @GenerateEntityKey annotations found.")
    }

    "generate mappings for a Workflow with GenerateEntityKey" in {
      assertDescriptor[WorkflowTestModels.WorkflowWithIdGenerator] { desc =>
        val method = desc.commandHandlers("StartTransfer")
        assertRequestFieldJavaType(method, "json_body", JavaType.MESSAGE)

        val keyGenerator = findKalixMethodOptions(desc, method.grpcMethodName).getEntity.getKeyGenerator
        keyGenerator shouldBe Generator.VERSION_4_UUID
      }
    }

    "generate mappings for a Workflow with workflow keys in path and JWT annotations" in {
      assertDescriptor[WorkflowTestModels.WorkflowWithJWT] { desc =>
        val method = desc.commandHandlers("StartTransfer")
        val fieldKey = "transferId"
        assertRequestFieldJavaType(method, fieldKey, JavaType.STRING)
        assertEntityKeyField(method, fieldKey)
        assertRequestFieldJavaType(method, "json_body", JavaType.MESSAGE)

        val jwtOption = findKalixMethodOptions(desc, method.grpcMethodName).getJwt
        jwtOption.getBearerTokenIssuer(0) shouldBe "a"
        jwtOption.getBearerTokenIssuer(1) shouldBe "b"
        jwtOption.getValidate(0) shouldBe JwtMethodMode.BEARER_TOKEN
      }
    }

    "generate ACL annotations at service level" in {
      assertDescriptor[WorkflowTestModels.WorkflowWithAcl] { desc =>
        val extension = desc.serviceDescriptor.getOptions.getExtension(kalix.Annotations.service)
        val service = extension.getAcl.getAllow(0).getService
        service shouldBe "test"
      }
    }

    "generate ACL annotations at method level" in {
      assertDescriptor[WorkflowTestModels.WorkflowWithMethodLevelAcl] { desc =>
        val extension = findKalixMethodOptions(desc, "StartTransfer")
        val service = extension.getAcl.getAllow(0).getService
        service shouldBe "test"
      }
    }
  }

}
