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

import com.google.protobuf.Descriptors.FieldDescriptor.JavaType
import kalix.JwtMethodOptions.JwtMethodMode
import kalix.JwtServiceOptions.JwtServiceMode
import kalix.KeyGeneratorMethodOptions.Generator
import kalix.javasdk.impl.reflection.ServiceIntrospectionException
import kalix.spring.testmodels.workflow.WorkflowTestModels.WorkflowWithAcl
import kalix.spring.testmodels.workflow.WorkflowTestModels.WorkflowWithIdGenerator
import kalix.spring.testmodels.workflow.WorkflowTestModels.WorkflowWithIllDefinedIdGenerator
import kalix.spring.testmodels.workflow.WorkflowTestModels.WorkflowWithKeyOverridden
import kalix.spring.testmodels.workflow.WorkflowTestModels.WorkflowWithMethodLevelAcl
import kalix.spring.testmodels.workflow.WorkflowTestModels.WorkflowWithMethodLevelJWT
import kalix.spring.testmodels.workflow.WorkflowTestModels.WorkflowWithMethodLevelKey
import kalix.spring.testmodels.workflow.WorkflowTestModels.WorkflowWithServiceLevelJWT
import kalix.spring.testmodels.workflow.WorkflowTestModels.WorkflowWithTypeLevelKey
import kalix.spring.testmodels.workflow.WorkflowTestModels.WorkflowWithoutIdGeneratorAndId
import org.scalatest.wordspec.AnyWordSpec

import scala.jdk.CollectionConverters.CollectionHasAsScala

class WorkflowEntityDescriptorFactorySpec extends AnyWordSpec with ComponentDescriptorSuite {

  "Workflow descriptor factory" should {
    "validate a Workflow must be declared as public" in {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[NotPublicComponents.NotPublicWorkflow]).failIfInvalid
      }.getMessage should include("NotPublicWorkflow is not marked with `public` modifier. Components must be public.")
    }

    "generate mappings for a Workflow with entity ids in path" in {
      assertDescriptor[WorkflowWithTypeLevelKey] { desc =>
        val method = desc.commandHandlers("StartTransfer")
        val fieldKey = "transferId"
        assertRequestFieldJavaType(method, fieldKey, JavaType.STRING)
        assertEntityIdField(method, fieldKey)
        assertRequestFieldJavaType(method, "json_body", JavaType.MESSAGE)
      }
    }

    "generate mappings for a Workflow with keys in path and EntityKey on method" in {
      assertDescriptor[WorkflowWithMethodLevelKey] { desc =>
        val method = desc.commandHandlers("StartTransfer")
        val fieldKey = "transferId"
        assertRequestFieldJavaType(method, fieldKey, JavaType.STRING)
        assertEntityIdField(method, fieldKey)
        assertRequestFieldJavaType(method, "json_body", JavaType.MESSAGE)
      }
    }

    "generate mappings for a Workflow with Id on method overrides EntityKey on type" in {
      assertDescriptor[WorkflowWithKeyOverridden] { desc =>
        val method = desc.commandHandlers("StartTransfer")
        val fieldKey = "transferId"
        assertRequestFieldJavaType(method, fieldKey, JavaType.STRING)
        assertEntityIdField(method, fieldKey)
        assertRequestFieldJavaType(method, "json_body", JavaType.MESSAGE)
      }
    }

    "fail if mix Id and GenerateId on method" in {
      intercept[ServiceIntrospectionException] {
        descriptorFor[WorkflowWithIllDefinedIdGenerator]
      }.getMessage should include("Invalid annotation usage. Found both @Id and @GenerateId annotations.")
    }

    "fail if no Id nor GenerateId is defined" in {
      intercept[ServiceIntrospectionException] {
        descriptorFor[WorkflowWithoutIdGeneratorAndId]
      }.getMessage should include("Invalid command method. No @Id nor @GenerateId annotations found.")
    }

    "generate mappings for a Workflow with GenerateId" in {
      assertDescriptor[WorkflowWithIdGenerator] { desc =>
        val method = desc.commandHandlers("StartTransfer")
        assertRequestFieldJavaType(method, "json_body", JavaType.MESSAGE)

        val generator = findKalixMethodOptions(desc, method.grpcMethodName).getIdGenerator.getAlgorithm
        generator shouldBe Generator.VERSION_4_UUID
      }
    }

    "generate mappings for a Workflow with workflow keys in path and method level JWT annotation" in {
      assertDescriptor[WorkflowWithMethodLevelJWT] { desc =>
        val method = desc.commandHandlers("StartTransfer")
        val fieldKey = "transferId"
        assertRequestFieldJavaType(method, fieldKey, JavaType.STRING)
        assertEntityIdField(method, fieldKey)
        assertRequestFieldJavaType(method, "json_body", JavaType.MESSAGE)

        val jwtOption = findKalixMethodOptions(desc, method.grpcMethodName).getJwt
        jwtOption.getBearerTokenIssuer(0) shouldBe "a"
        jwtOption.getBearerTokenIssuer(1) shouldBe "b"
        jwtOption.getValidate(0) shouldBe JwtMethodMode.BEARER_TOKEN

        val Seq(claim1, claim2) = jwtOption.getStaticClaimList.asScala.toSeq
        claim1.getClaim shouldBe "role"
        claim1.getValue(0) shouldBe "method-admin"
        claim2.getClaim shouldBe "aud"
        claim2.getValue(0) shouldBe "${ENV}.kalix.io"
      }
    }

    "generate mappings for a Workflow with workflow keys in path and service level JWT annotation" in {
      assertDescriptor[WorkflowWithServiceLevelJWT] { desc =>
        val extension = desc.serviceDescriptor.getOptions.getExtension(kalix.Annotations.service)
        val jwtOption = extension.getJwt
        jwtOption.getBearerTokenIssuer(0) shouldBe "c"
        jwtOption.getBearerTokenIssuer(1) shouldBe "d"
        jwtOption.getValidate shouldBe JwtServiceMode.BEARER_TOKEN

        val Seq(claim1, claim2) = jwtOption.getStaticClaimList.asScala.toSeq
        claim1.getClaim shouldBe "role"
        claim1.getValue(0) shouldBe "admin"
        claim2.getClaim shouldBe "aud"
        claim2.getValue(0) shouldBe "${ENV}"
      }
    }

    "generate ACL annotations at service level" in {
      assertDescriptor[WorkflowWithAcl] { desc =>
        val extension = desc.serviceDescriptor.getOptions.getExtension(kalix.Annotations.service)
        val service = extension.getAcl.getAllow(0).getService
        service shouldBe "test"
      }
    }

    "generate ACL annotations at method level" in {
      assertDescriptor[WorkflowWithMethodLevelAcl] { desc =>
        val extension = findKalixMethodOptions(desc, "StartTransfer")
        val service = extension.getAcl.getAllow(0).getService
        service shouldBe "test"
      }
    }
  }

}
