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

import kalix.springsdk.testmodels.view.ViewTestModels.{
  TransformMethodLackingEventParam,
  TransformMethodWrongParamOrder,
  TransformedUserView,
  TransformedUserViewUsingState,
  UserByEmailWithGet,
  UserByEmailWithPost,
  UserByEmailWithPostRequestBodyOnly,
  UserByNameEmailWithPost,
  UserByNameStreamed,
  ViewWithNoQuery,
  ViewWithSubscriptionsInMixedLevels,
  ViewWithTwoQueries
}
import org.scalatest.wordspec.AnyWordSpec

class ViewDescriptorFactorySpec extends AnyWordSpec with ComponentDescriptorSuite {

  "View descriptor factory" should {

    "not allow @Subscribe annotations in mixed levels" in {
      // it should be annotated either on type or on method level
      intercept[InvalidComponentException] {
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
        methodOptions.getView.getJsonSchema.getOutput shouldBe "TransformedUser"

        val queryMethodOptions = this.findKalixMethodOptions(desc, "GetUser")
        queryMethodOptions.getView.getQuery.getQuery shouldBe "SELECT * FROM users_view WHERE email = :email"
        queryMethodOptions.getView.getJsonSchema.getJsonBodyInputField shouldBe "json_body"
        queryMethodOptions.getView.getJsonSchema.getInput shouldBe "ByEmail"
        queryMethodOptions.getView.getJsonSchema.getOutput shouldBe "TransformedUser"

        val tableMessageDescriptor = desc.fileDescriptor.findMessageTypeByName("TransformedUser")
        tableMessageDescriptor should not be null

        val rule = findHttpRule(desc, "GetUser")
        rule.getPost shouldBe "/users/by-email"
      }
    }

    "generate proto for a View using POST request with explicit update method that also receives the current state" in {
      assertDescriptor[TransformedUserViewUsingState] { desc =>
        val methodOptions = this.findKalixMethodOptions(desc, "OnChange")
        val entityType = methodOptions.getEventing.getIn.getValueEntity
        entityType shouldBe "user"

        methodOptions.getView.getUpdate.getTable shouldBe "users_view"
        methodOptions.getView.getUpdate.getTransformUpdates shouldBe true
        methodOptions.getView.getJsonSchema.getOutput shouldBe "TransformedUser"

        val queryMethodOptions = this.findKalixMethodOptions(desc, "GetUser")
        queryMethodOptions.getView.getQuery.getQuery shouldBe "SELECT * FROM users_view WHERE email = :email"
        queryMethodOptions.getView.getJsonSchema.getJsonBodyInputField shouldBe "json_body"
        queryMethodOptions.getView.getJsonSchema.getInput shouldBe "ByEmail"
        queryMethodOptions.getView.getJsonSchema.getOutput shouldBe "TransformedUser"

        val tableMessageDescriptor = desc.fileDescriptor.findMessageTypeByName("TransformedUser")
        tableMessageDescriptor should not be null

        val rule = findHttpRule(desc, "GetUser")
        rule.getPost shouldBe "/users/by-email"

        desc.methods("OnChange").parameterExtractors.length shouldBe 1
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
        methodOptions.getView.getJsonSchema.getOutput shouldBe "User"

        val queryMethodOptions = this.findKalixMethodOptions(desc, "GetUser")
        queryMethodOptions.getView.getQuery.getQuery shouldBe "SELECT * FROM users_view WHERE email = :email"
        queryMethodOptions.getView.getJsonSchema.getOutput shouldBe "User"
        queryMethodOptions.getView.getJsonSchema.getJsonBodyInputField shouldBe "json_body"
        queryMethodOptions.getView.getJsonSchema.getInput shouldBe "ByEmail"

        val tableMessageDescriptor = desc.fileDescriptor.findMessageTypeByName("User")
        tableMessageDescriptor should not be null

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
        methodOptions.getView.getJsonSchema.getOutput shouldBe "User"
        // check json input schema:  ByEmail

        val queryMethodOptions = this.findKalixMethodOptions(desc, "GetUser")
        queryMethodOptions.getView.getQuery.getQuery shouldBe "SELECT * FROM users_view WHERE email = :email"
        queryMethodOptions.getView.getJsonSchema.getOutput shouldBe "User"
        queryMethodOptions.getView.getJsonSchema.getJsonBodyInputField shouldBe "json_body"
        queryMethodOptions.getView.getJsonSchema.getInput shouldBe "ByEmail"

        desc.fileDescriptor.findMessageTypeByName("User") should not be null
        desc.fileDescriptor.findMessageTypeByName("ByEmail") should not be null

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
        methodOptions.getView.getJsonSchema.getOutput shouldBe "User"

        val queryMethodOptions = this.findKalixMethodOptions(desc, "GetUser")
        queryMethodOptions.getView.getQuery.getQuery shouldBe "SELECT * FROM users_view WHERE email = :email"
        queryMethodOptions.getView.getJsonSchema.getOutput shouldBe "User"
        // not defined when query body not used
        queryMethodOptions.getView.getJsonSchema.getJsonBodyInputField shouldBe ""
        queryMethodOptions.getView.getJsonSchema.getInput shouldBe ""

        val tableMessageDescriptor =
          desc.fileDescriptor.findMessageTypeByName("User")
        tableMessageDescriptor should not be null

        val rule = findHttpRule(desc, "GetUser")
        rule.getGet shouldBe "/users/{email}"
      }
    }

    "generate proto for a View using POST request with only request body" in {
      assertDescriptor[UserByEmailWithPostRequestBodyOnly] { desc =>
        val methodOptions = this.findKalixMethodOptions(desc, "OnChange")
        val entityType = methodOptions.getEventing.getIn.getValueEntity
        entityType shouldBe "user"

        methodOptions.getView.getUpdate.getTable shouldBe "users_view"
        methodOptions.getView.getUpdate.getTransformUpdates shouldBe false
        // check json input schema:  ByEmail
        methodOptions.getView.getJsonSchema.getOutput shouldBe "User"

        val queryMethodOptions = this.findKalixMethodOptions(desc, "GetUser")
        queryMethodOptions.getView.getQuery.getQuery shouldBe "SELECT * FROM users_view WHERE email = :email"
        queryMethodOptions.getView.getJsonSchema.getOutput shouldBe "User"
        queryMethodOptions.getView.getJsonSchema.getJsonBodyInputField shouldBe "json_body"
        // based on the body parameter type class name
        queryMethodOptions.getView.getJsonSchema.getInput shouldBe "ByEmail"

        val tableMessageDescriptor =
          desc.fileDescriptor.findMessageTypeByName("User")
        tableMessageDescriptor should not be null

        val rule = findHttpRule(desc, "GetUser")
        rule.getPost shouldBe "/users/by-email"
      }
    }

    "generate proto for a View using GET request with path param and returning stream" in {
      assertDescriptor[UserByNameStreamed] { desc =>
        val methodOptions = this.findKalixMethodOptions(desc, "OnChange")
        val entityType = methodOptions.getEventing.getIn.getValueEntity
        entityType shouldBe "user"

        methodOptions.getView.getUpdate.getTable shouldBe "users_view"
        methodOptions.getView.getUpdate.getTransformUpdates shouldBe false
        methodOptions.getView.getJsonSchema.getOutput shouldBe "User"

        val methodDescriptor = desc.serviceDescriptor.findMethodByName("GetUser")
        methodDescriptor.isServerStreaming shouldBe true

        val queryMethodOptions = this.findKalixMethodOptions(desc, "GetUser")
        queryMethodOptions.getView.getQuery.getQuery shouldBe "SELECT * FROM users_view WHERE name = :name"
        queryMethodOptions.getView.getJsonSchema.getOutput shouldBe "User"
        // not defined when query body not used
        queryMethodOptions.getView.getJsonSchema.getJsonBodyInputField shouldBe ""
        queryMethodOptions.getView.getJsonSchema.getInput shouldBe ""

        val tableMessageDescriptor =
          desc.fileDescriptor.findMessageTypeByName("User")
        tableMessageDescriptor should not be null

        val rule = findHttpRule(desc, "GetUser")
        rule.getGet shouldBe "/users/{name}"

      }
    }
    "fail if no query method found" in {
      intercept[InvalidComponentException] {
        ComponentDescriptor.descriptorFor[ViewWithNoQuery]
      }
    }

    "fail if more than one query method is found" in {
      intercept[InvalidComponentException] {
        ComponentDescriptor.descriptorFor[ViewWithTwoQueries]
      }
    }

    "fail if transform method has wrong signature lacking the event param" in {
      intercept[InvalidComponentException] {
        ComponentDescriptor.descriptorFor[TransformMethodLackingEventParam]
      }
    }

    "fail if transform method has wrong signature because of parameters order" in {
      intercept[InvalidComponentException] {
        ComponentDescriptor.descriptorFor[TransformMethodWrongParamOrder]
      }
    }

  }
}
