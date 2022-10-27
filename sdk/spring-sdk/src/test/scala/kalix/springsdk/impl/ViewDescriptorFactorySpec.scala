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
import kalix.springsdk.impl.reflection.ServiceIntrospectionException
import kalix.springsdk.testmodels.subscriptions.PubSubTestModels.SubscribeOnTypeToEventSourcedEventsWithMethodWithState
import kalix.springsdk.testmodels.view.ViewTestModels.IllDefineUserByEmailWithStreamUpdates
import kalix.springsdk.testmodels.view.ViewTestModels.SubscribeToEventSourcedEvents
import kalix.springsdk.testmodels.view.ViewTestModels.SubscribeToEventSourcedEventsWithMethodWithState
import kalix.springsdk.testmodels.view.ViewTestModels.TransformedUserView
import kalix.springsdk.testmodels.view.ViewTestModels.TransformedUserViewUsingState
import kalix.springsdk.testmodels.view.ViewTestModels.TransformedUserViewWithJWT
import kalix.springsdk.testmodels.view.ViewTestModels.UserByEmailWithGet
import kalix.springsdk.testmodels.view.ViewTestModels.UserByEmailWithPost
import kalix.springsdk.testmodels.view.ViewTestModels.UserByEmailWithPostRequestBodyOnly
import kalix.springsdk.testmodels.view.ViewTestModels.UserByEmailWithStreamUpdates
import kalix.springsdk.testmodels.view.ViewTestModels.UserByNameEmailWithPost
import kalix.springsdk.testmodels.view.ViewTestModels.UserByNameStreamed
import kalix.springsdk.testmodels.view.ViewTestModels.ViewWithMethodLevelAcl
import kalix.springsdk.testmodels.view.ViewTestModels.ViewWithNoQuery
import kalix.springsdk.testmodels.view.ViewTestModels.ViewWithServiceLevelAcl
import kalix.springsdk.testmodels.view.ViewTestModels.ViewWithSubscriptionsInMixedLevels
import kalix.springsdk.testmodels.view.ViewTestModels.ViewWithTwoQueries
import org.scalatest.wordspec.AnyWordSpec

class ViewDescriptorFactorySpec extends AnyWordSpec with ComponentDescriptorSuite {

  "View descriptor factory" should {

    "generate ACL annotations at service level" in {
      assertDescriptor[ViewWithServiceLevelAcl] { desc =>
        val extension = desc.serviceDescriptor.getOptions.getExtension(kalix.Annotations.service)
        val service = extension.getAcl.getAllow(0).getService
        service shouldBe "test"
      }
    }

    "generate ACL annotations at method level" in {
      assertDescriptor[ViewWithMethodLevelAcl] { desc =>
        val extension = findKalixMethodOptions(desc, "GetEmployeeByEmail")
        val service = extension.getAcl.getAllow(0).getService
        service shouldBe "test"
      }
    }

    "generate query with stream updates enabled" in {
      assertDescriptor[UserByEmailWithStreamUpdates] { desc =>
        val queryMethodOptions = this.findKalixMethodOptions(desc, "GetUser")
        queryMethodOptions.getView.getQuery.getQuery shouldBe "SELECT * FROM users_view WHERE email = :email"
        queryMethodOptions.getView.getJsonSchema.getOutput shouldBe "User"
        queryMethodOptions.getView.getJsonSchema.getJsonBodyInputField shouldBe "json_body"
        queryMethodOptions.getView.getJsonSchema.getInput shouldBe "ByEmail"

        val streamUpdates = queryMethodOptions.getView.getQuery.getStreamUpdates
        streamUpdates shouldBe true
      }
    }

    "fail when using stream updates in unary methods" in {
      intercept[ServiceIntrospectionException] {
        descriptorFor[IllDefineUserByEmailWithStreamUpdates]
      }
    }
  }

  "View descriptor factory (for Value Entity)" should {

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

    "generate proto for a View using POST request with explicit update method and JWT Kalix annotations" in {
      assertDescriptor[TransformedUserViewWithJWT] { desc =>

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

        val method = desc.commandHandlers("GetUser")
        val jwtOption = findKalixMethodOptions(desc, method.grpcMethodName).getJwt
        jwtOption.getBearerTokenIssuer(0) shouldBe "a"
        jwtOption.getBearerTokenIssuer(1) shouldBe "b"
        jwtOption.getValidate(0) shouldBe JwtMethodMode.BEARER_TOKEN
        jwtOption.getSign(0) shouldBe JwtMethodMode.MESSAGE
        assertRequestFieldJavaType(method, "json_body", JavaType.MESSAGE)
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

        val javaMethod = desc.commandHandlers("OnChange").methodInvokers.values.head
        javaMethod.parameterExtractors.length shouldBe 1
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
        descriptorFor[ViewWithNoQuery]
      }
    }

    "fail if more than one query method is found" in {
      intercept[InvalidComponentException] {
        descriptorFor[ViewWithTwoQueries]
      }
    }
  }

  "View descriptor factory (for Event Sourced Entity)" should {

    "generate proto for a View using POST request" in {
      assertDescriptor[SubscribeToEventSourcedEvents] { desc =>

        val methodOptions = this.findKalixMethodOptions(desc, "OnEvent")
        val entityType = methodOptions.getEventing.getIn.getEventSourcedEntity
        entityType shouldBe "employee"

        methodOptions.getView.getUpdate.getTable shouldBe "employees_view"
        methodOptions.getView.getUpdate.getTransformUpdates shouldBe true
        methodOptions.getView.getJsonSchema.getOutput shouldBe "Employee"

        val queryMethodOptions = this.findKalixMethodOptions(desc, "GetEmployeeByEmail")
        queryMethodOptions.getView.getQuery.getQuery shouldBe "SELECT * FROM employees_view WHERE email = :email"
        queryMethodOptions.getView.getJsonSchema.getOutput shouldBe "Employee"
        // not defined when query body not used
        queryMethodOptions.getView.getJsonSchema.getJsonBodyInputField shouldBe ""
        queryMethodOptions.getView.getJsonSchema.getInput shouldBe ""

        val tableMessageDescriptor = desc.fileDescriptor.findMessageTypeByName("Employee")
        tableMessageDescriptor should not be null

        val rule = findHttpRule(desc, "GetEmployeeByEmail")
        rule.getPost shouldBe "/employees/by-email/{email}"
      }
    }

    "generate proto for a View using POST request with subscription method accepting state" in {
      assertDescriptor[SubscribeToEventSourcedEventsWithMethodWithState] { desc =>

        val methodOptions = this.findKalixMethodOptions(desc, "OnEvent")
        val entityType = methodOptions.getEventing.getIn.getEventSourcedEntity
        entityType shouldBe "employee"

        methodOptions.getView.getUpdate.getTable shouldBe "employees_view"
        methodOptions.getView.getUpdate.getTransformUpdates shouldBe true
        methodOptions.getView.getJsonSchema.getOutput shouldBe "Employee"

        val queryMethodOptions = this.findKalixMethodOptions(desc, "GetEmployeeByEmail")
        queryMethodOptions.getView.getQuery.getQuery shouldBe "SELECT * FROM employees_view WHERE email = :email"
        queryMethodOptions.getView.getJsonSchema.getOutput shouldBe "Employee"
        // not defined when query body not used
        queryMethodOptions.getView.getJsonSchema.getJsonBodyInputField shouldBe ""
        queryMethodOptions.getView.getJsonSchema.getInput shouldBe ""

        val tableMessageDescriptor = desc.fileDescriptor.findMessageTypeByName("Employee")
        tableMessageDescriptor should not be null

        val rule = findHttpRule(desc, "GetEmployeeByEmail")
        rule.getPost shouldBe "/employees/by-email/{email}"
      }
    }

    "generate proto for a View with multiple methods to handle different events" in {
      assertDescriptor[SubscribeOnTypeToEventSourcedEventsWithMethodWithState] { desc =>
        val methodOptions = this.findKalixMethodOptions(desc, "KalixSyntheticMethodOnESEmployee")

        val entityType = methodOptions.getEventing.getIn.getEventSourcedEntity
        entityType shouldBe "employee"

        methodOptions.getView.getUpdate.getTable shouldBe "employee_table"
        methodOptions.getView.getUpdate.getTransformUpdates shouldBe true
        methodOptions.getView.getJsonSchema.getOutput shouldBe "Employee"
        methodOptions.getEventing.getIn.getIgnore shouldBe false // we don't set the property so the proxy won't ignore. Ignore is only internal to the SDK

      }
    }
  }

}
