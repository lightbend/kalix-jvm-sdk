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

package kalix.springsdk.impl.action

import com.google.protobuf.Descriptors.FieldDescriptor.JavaType
import kalix.springsdk.impl.IntrospectionSuite
import kalix.springsdk.testmodels.action.ActionsTestModels.DeleteWithOneParam
import kalix.springsdk.testmodels.action.ActionsTestModels.GetClassLevel
import kalix.springsdk.testmodels.action.ActionsTestModels.GetWithOneParam
import kalix.springsdk.testmodels.action.ActionsTestModels.GetWithoutParam
import kalix.springsdk.testmodels.action.ActionsTestModels.PatchWithOneParam
import kalix.springsdk.testmodels.action.ActionsTestModels.PatchWithoutParam
import kalix.springsdk.testmodels.action.ActionsTestModels.PostWithOneParam
import kalix.springsdk.testmodels.action.ActionsTestModels.PostWithTwoMethods
import kalix.springsdk.testmodels.action.ActionsTestModels.PostWithTwoParam
import kalix.springsdk.testmodels.action.ActionsTestModels.PostWithoutParam
import kalix.springsdk.testmodels.action.ActionsTestModels.PutWithOneParam
import kalix.springsdk.testmodels.action.ActionsTestModels.PutWithoutParam
import org.scalatest.wordspec.AnyWordSpec

class ActionIntrospectorSpec extends AnyWordSpec with IntrospectionSuite {

  "Action introspector" should {

    "generate mappings for an Action with GET without path param" in {
      assertDescriptor[GetWithoutParam] { desc =>
        val method = desc.methods("Message")
        method.messageDescriptor.getFields.size() shouldBe 0
      }
    }

    "generate mappings for an Action with GET and one path param" in {
      assertDescriptor[GetWithOneParam] { desc =>
        val method = desc.methods("Message")
        assertMessage(method, "one", JavaType.STRING)
      }
    }

    "generate mappings for an Action with class level Request mapping" in {
      assertDescriptor[GetClassLevel] { desc =>
        val method = desc.methods("Message")
        assertMessage(method, "one", JavaType.STRING)
        assertMessage(method, "two", JavaType.LONG)
      }
    }

    "generate mappings for an Action with POST without path param" in {
      assertDescriptor[PostWithoutParam] { desc =>
        val method = desc.methods("Message")
        assertMessage(method, "json_body", JavaType.MESSAGE)
      }
    }

    "generate mappings for an Action with POST and one path param" in {
      assertDescriptor[PostWithOneParam] { desc =>
        val method = desc.methods("Message")
        assertMessage(method, "one", JavaType.STRING)
        assertMessage(method, "json_body", JavaType.MESSAGE)
      }
    }

    "generate mappings for an Action with POST and two path param" in {
      assertDescriptor[PostWithTwoParam] { desc =>
        val method = desc.methods("Message")
        assertMessage(method, "one", JavaType.STRING)
        assertMessage(method, "two", JavaType.LONG)
        assertMessage(method, "json_body", JavaType.MESSAGE)
      }
    }

    "generate mappings for an Action with POST and two methods" in {
      assertDescriptor[PostWithTwoMethods] { desc =>

        val firstMethod = desc.methods("Message")
        assertMessage(firstMethod, "text", JavaType.STRING)
        assertMessage(firstMethod, "json_body", JavaType.MESSAGE)

        val secondMethod = desc.methods("Message1")
        assertMessage(secondMethod, "num", JavaType.LONG)
        assertMessage(secondMethod, "json_body", JavaType.MESSAGE)

      }
    }

    "generate mappings for an Action with PUT without path param" in {
      assertDescriptor[PutWithoutParam] { desc =>
        val method = desc.methods("Message")
        assertMessage(method, "json_body", JavaType.MESSAGE)
      }
    }

    "generate mappings for an Action with PUT and one path param" in {
      assertDescriptor[PutWithOneParam] { desc =>
        val method = desc.methods("Message")
        assertMessage(method, "one", JavaType.STRING)
      }
    }

    "generate mappings for an Action with PATCH without path param" in {
      assertDescriptor[PatchWithoutParam] { desc =>
        val method = desc.methods("Message")
        assertMessage(method, "json_body", JavaType.MESSAGE)
      }
    }

    "generate mappings for an Action with PATCH and one path param" in {
      assertDescriptor[PatchWithOneParam] { desc =>
        val method = desc.methods("Message")
        assertMessage(method, "one", JavaType.STRING)
      }
    }

    "generate mappings for an Action with DELETE and one path param" in {
      assertDescriptor[DeleteWithOneParam] { desc =>
        val method = desc.methods("Message")
        assertMessage(method, "one", JavaType.STRING)
      }
    }

  }

}
