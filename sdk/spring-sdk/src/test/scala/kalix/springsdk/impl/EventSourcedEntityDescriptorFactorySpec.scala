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
import kalix.springsdk.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.WellAnnotatedESEntity
import org.scalatest.wordspec.AnyWordSpec

class EventSourcedEntityDescriptorFactorySpec extends AnyWordSpec with ComponentDescriptorSuite {

  "EventSourced descriptor factory" should {
    "generate mappings for a Event Sourced with entity keys in path" in {
      assertDescriptor[WellAnnotatedESEntity] { desc =>
        val method = desc.methods("GetInteger")
        assertRequestFieldJavaType(method, "id", JavaType.STRING)
        assertEntityKeyField(method, "id")
        assertRequestFieldJavaType(method, "number", JavaType.INT)

        val postMethod = desc.methods("ChangeInteger")
        assertRequestFieldJavaType(postMethod, "id", JavaType.STRING)
        assertEntityKeyField(postMethod, "id")
        assertRequestFieldJavaType(postMethod, "number", JavaType.INT)
      }
    }
  }

}
