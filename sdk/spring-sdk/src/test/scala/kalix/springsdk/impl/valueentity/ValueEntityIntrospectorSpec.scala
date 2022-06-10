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

package kalix.springsdk.impl.valueentity

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType
import kalix.springsdk.impl.IntrospectionSuite
import kalix.springsdk.impl.Introspector
import kalix.springsdk.impl.reflection.NameGenerator
import kalix.springsdk.valueentity.RestAnnotatedValueEntities.PostWithEntityKeys
import kalix.springsdk.valueentity.RestAnnotatedValueEntities.ValueEntityUsingJavaSdk
import org.scalatest.wordspec.AnyWordSpec

class ValueEntityIntrospectorSpec extends AnyWordSpec with IntrospectionSuite {

  "ValueEntity introspector" should {
    "generate mappings for a Value Entity with entity keys in path" in {
      assertDescriptor[PostWithEntityKeys] { desc =>
        val method = desc.methods("CreateEntity")
        assertMessage(method, "json_body", JavaType.MESSAGE)

        assertMessage(method, "userId", JavaType.STRING)
        assertEntityKeyField(method, "userId")

        assertMessage(method, "cartId", JavaType.STRING)
        assertEntityKeyField(method, "cartId")
      }
    }

    "fail when inspecting a ValueEntity implementing Java SDK directly" in {
      intercept[IllegalArgumentException] {
        Introspector.inspect(classOf[ValueEntityUsingJavaSdk], new NameGenerator, new ObjectMapper())
      }
    }

  }

}
