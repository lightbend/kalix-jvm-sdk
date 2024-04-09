/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl

import com.google.protobuf.Any
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType
import kalix.JwtMethodOptions.JwtMethodMode
import kalix.JwtServiceOptions.JwtServiceMode
import kalix.KeyGeneratorMethodOptions
import kalix.spring.testmodels.valueentity.Counter
import kalix.spring.testmodels.valueentity.ValueEntitiesTestModels.GetWithQueryParams
import kalix.spring.testmodels.valueentity.ValueEntitiesTestModels.PostWithIds
import kalix.spring.testmodels.valueentity.ValueEntitiesTestModels.PostWithIdsIncorrectOrder
import kalix.spring.testmodels.valueentity.ValueEntitiesTestModels.PostWithIdsMissingParams
import kalix.spring.testmodels.valueentity.ValueEntitiesTestModels.ValueEntityWithMethodLevelAcl
import kalix.spring.testmodels.valueentity.ValueEntitiesTestModels.ValueEntityWithMethodLevelJwt
import kalix.spring.testmodels.valueentity.ValueEntitiesTestModels.ValueEntityWithServiceLevelAcl
import kalix.spring.testmodels.valueentity.ValueEntitiesTestModels.ValueEntityWithServiceLevelJwt
import org.scalatest.wordspec.AnyWordSpec

import scala.jdk.CollectionConverters.CollectionHasAsScala

class ValueEntityDescriptorFactorySpec extends AnyWordSpec with ComponentDescriptorSuite {

  "ValueEntity descriptor factory" should {
    "validate a ValueEntity must be declared as public" in {
      intercept[InvalidComponentException] {
        Validations.validate(classOf[NotPublicComponents.NotPublicValueEntity]).failIfInvalid
      }.getMessage should include(
        "NotPublicValueEntity is not marked with `public` modifier. Components must be public.")
    }

    "generate mappings for a Value Entity with entity ids in path" in {
      assertDescriptor[PostWithIds] { desc =>
        val method = desc.commandHandlers("CreateEntity")
        assertRequestFieldJavaType(method, "json_body", JavaType.MESSAGE)
        assertRequestFieldMessageType(method, "json_body", Any.getDescriptor.getFullName)

        assertRequestFieldJavaType(method, "userId", JavaType.STRING)
        assertEntityIdField(method, "userId")

        assertRequestFieldJavaType(method, "cartId", JavaType.STRING)
        assertEntityIdField(method, "cartId")
      }
    }

    "generate mappings for a Value Entity with generated id" in {
      assertDescriptor[Counter] { desc =>
        val method = desc.commandHandlers("RandomIncrease")
        assertRequestFieldNumberAndJavaType(method, "value", 2, JavaType.INT)

        val extension = findKalixMethodOptions(desc, "RandomIncrease")
        extension.getIdGenerator.getAlgorithm shouldBe KeyGeneratorMethodOptions.Generator.VERSION_4_UUID
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
        assertRequestFieldNumberAndJavaType(createMethod, "userId", 2, JavaType.STRING)
        assertRequestFieldNumberAndJavaType(createMethod, "cartId", 3, JavaType.STRING)
        assertRequestFieldNumberAndJavaType(createMethod, "otherParam", 4, JavaType.INT)
        assertRequestFieldNumberAndJavaType(createMethod, "someParam", 5, JavaType.STRING)
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

    "generate descriptor for ValueEntity with service level JWT annotation" in {
      assertDescriptor[ValueEntityWithServiceLevelJwt] { desc =>
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

    "generate descriptor for ValueEntity with method level JWT annotation" in {
      assertDescriptor[ValueEntityWithMethodLevelJwt] { desc =>
        val jwtOption = findKalixMethodOptions(desc, "CreateEntity").getJwt
        jwtOption.getBearerTokenIssuer(0) shouldBe "c"
        jwtOption.getBearerTokenIssuer(1) shouldBe "d"
        jwtOption.getValidate(0) shouldBe JwtMethodMode.BEARER_TOKEN
        val Seq(claim1, claim2) = jwtOption.getStaticClaimList.asScala.toSeq
        claim1.getClaim shouldBe "role"
        claim1.getValue(0) shouldBe "method-admin"
        claim2.getClaim shouldBe "aud"
        claim2.getValue(0) shouldBe "${ENV}"
      }
    }

    "not allow different order of entity ids in the path" in {
      // it should be annotated either on type or on method level
      intercept[InvalidComponentException] {
        Validations.validate(classOf[PostWithIdsIncorrectOrder]).failIfInvalid
      }.getMessage should include(
        "Ids in the path '/user/{cartId}/{userId}/create' are in a different order than specified in the @Id annotation [userId, cartId]. This could lead to unexpected bugs when calling the component.")
    }

    "not allow missing ids in the path" in {
      // it should be annotated either on type or on method level
      intercept[InvalidComponentException] {
        Validations.validate(classOf[PostWithIdsMissingParams]).failIfInvalid
      }.getMessage should include(
        "All ids [userId, cartId] should be used in the path '/user/{cartId}/create'. Missing ids [userId].")
    }
  }

}
