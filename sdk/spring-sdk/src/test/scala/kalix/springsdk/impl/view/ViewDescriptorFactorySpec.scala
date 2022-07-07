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

package kalix.springsdk.impl.view

import kalix.springsdk.impl.{ ComponentDescriptor, ComponentDescriptorSuite, ViewDescriptorFactory }
import kalix.springsdk.testmodels.view.ViewTestModels.{
  TransformedUserView,
  UserByEmailWithGet,
  UserByEmailWithPost,
  UserByNameEmailWithPost,
  ViewWithNoQuery,
  ViewWithSubscriptionsInMixedLevels,
  ViewWithTwoQueries
}
import org.scalatest.wordspec.AnyWordSpec

class ViewDescriptorFactorySpec extends AnyWordSpec with ComponentDescriptorSuite {

  "View descriptor factory" should {

    "not allow @Subscribe annotations in mixed levels" in {
      // it should be annotated either on type or on method level
      intercept[IllegalArgumentException] {
        assertDescriptor[ViewWithSubscriptionsInMixedLevels] { desc => }
      }.getMessage should startWith("Mixed usage of @Subscribe.ValueEntity annotations.")
    }

    "generate proto for a View using POST request with explicit update method" in {
      assertDescriptor[TransformedUserView] { desc =>

        val methodOptions = this.findKalixMethodOptions(desc, "OnChange")
        val entityType = methodOptions.getEventing.getIn.getValueEntity
        entityType shouldBe "user"

        methodOptions.getView.getUpdate.getTable shouldBe "users_view"
        methodOptions.getView.getUpdate.getTransformUpdates shouldBe true
        // check json input schema:  ByEmail
        methodOptions.getView.getJsonSchema.getOutput shouldBe "kalix.springsdk.testmodels.view.TransformedUser"

        val queryMethodOptions = this.findKalixMethodOptions(desc, "GetUser")
        queryMethodOptions.getView.getQuery.getQuery shouldBe "SELECT * FROM users_view WHERE email = :email"
        queryMethodOptions.getView.getJsonSchema.getOutput shouldBe "kalix.springsdk.testmodels.view.TransformedUser"

        val rule = findHttpRule(desc, "GetUser")
        rule.getPost shouldBe "/users/by-email"

      }
    }

    "generate proto for a View using POST request" in {
      assertDescriptor[UserByEmailWithPost] { desc =>
        val methodOptions = this.findKalixMethodOptions(desc, "OnChange")
        val entityType = methodOptions.getEventing.getIn.getValueEntity
        entityType shouldBe "user"

        methodOptions.getView.getUpdate.getTable shouldBe "users_view"
        methodOptions.getView.getUpdate.getTransformUpdates shouldBe false
        // check json input schema:  ByEmail
        methodOptions.getView.getJsonSchema.getOutput shouldBe "kalix.springsdk.testmodels.valueentity.User"

        val queryMethodOptions = this.findKalixMethodOptions(desc, "GetUser")
        queryMethodOptions.getView.getQuery.getQuery shouldBe "SELECT * FROM users_view WHERE email = :email"
        queryMethodOptions.getView.getJsonSchema.getOutput shouldBe "kalix.springsdk.testmodels.valueentity.User"

        val rule = findHttpRule(desc, "GetUser")
        rule.getPost shouldBe "/users/by-email"
      }
    }

    "generate proto for a View using POST request with path param " in {
      assertDescriptor[UserByNameEmailWithPost] { desc =>
        val methodOptions = this.findKalixMethodOptions(desc, "OnChange")
        val entityType = methodOptions.getEventing.getIn.getValueEntity
        entityType shouldBe "user"

        methodOptions.getView.getUpdate.getTable shouldBe "users_view"
        methodOptions.getView.getUpdate.getTransformUpdates shouldBe false
        methodOptions.getView.getJsonSchema.getOutput shouldBe "kalix.springsdk.testmodels.valueentity.User"
        // check json input schema:  ByEmail

        val queryMethodOptions = this.findKalixMethodOptions(desc, "GetUser")
        queryMethodOptions.getView.getQuery.getQuery shouldBe "SELECT * FROM users_view WHERE email = :email"
        queryMethodOptions.getView.getJsonSchema.getOutput shouldBe "kalix.springsdk.testmodels.valueentity.User"

        val rule = findHttpRule(desc, "GetUser")
        rule.getPost shouldBe "/users/{name}/by-email"
      }
    }

    "generate proto for a View using GET request with path param" in {
      assertDescriptor[UserByEmailWithGet] { desc =>
        val methodOptions = this.findKalixMethodOptions(desc, "OnChange")
        val entityType = methodOptions.getEventing.getIn.getValueEntity
        entityType shouldBe "user"

        methodOptions.getView.getUpdate.getTable shouldBe "users_view"
        methodOptions.getView.getUpdate.getTransformUpdates shouldBe false
        // check json input schema:  ByEmail
        methodOptions.getView.getJsonSchema.getOutput shouldBe "kalix.springsdk.testmodels.valueentity.User"

        val queryMethodOptions = this.findKalixMethodOptions(desc, "GetUser")
        queryMethodOptions.getView.getQuery.getQuery shouldBe "SELECT * FROM users_view WHERE email = :email"
        queryMethodOptions.getView.getJsonSchema.getOutput shouldBe "kalix.springsdk.testmodels.valueentity.User"

        val rule = findHttpRule(desc, "GetUser")
        rule.getGet shouldBe "/users/{email}"
      }
    }

    "fail if no query method found" in {
      intercept[IllegalArgumentException] {
        ComponentDescriptor.descriptorFor[ViewWithNoQuery]
      }
    }

    "fail if more than one query method is found" in {
      intercept[IllegalArgumentException] {
        ComponentDescriptor.descriptorFor[ViewWithTwoQueries]
      }
    }

  }
}
