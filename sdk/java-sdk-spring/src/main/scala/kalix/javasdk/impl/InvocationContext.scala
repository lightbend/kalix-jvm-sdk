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

import com.google.protobuf.Descriptors
import com.google.protobuf.DynamicMessage
import com.google.protobuf.any.{ Any => ScalaPbAny }
import kalix.javasdk.JsonSupport
import kalix.javasdk.Metadata
import kalix.javasdk.impl.AnySupport.BytesPrimitive
import kalix.javasdk.impl.reflection.DynamicMessageContext
import kalix.javasdk.impl.reflection.MetadataContext

object InvocationContext {

  private val typeUrlField = ScalaPbAny.javaDescriptor.findFieldByName("type_url")
  private val valueField = ScalaPbAny.javaDescriptor.findFieldByName("value")

  def apply(
      anyMessage: ScalaPbAny,
      methodDescriptor: Descriptors.Descriptor,
      metadata: Metadata = Metadata.EMPTY): InvocationContext = {

    val dynamicMessage =
      if (anyMessage.typeUrl.startsWith(JsonSupport.KALIX_JSON) ||
        anyMessage.typeUrl == BytesPrimitive.fullName) {
        DynamicMessage
          .newBuilder(methodDescriptor)
          .setField(typeUrlField, anyMessage.typeUrl)
          .setField(valueField, anyMessage.value)
          .build()

      } else {
        DynamicMessage.parseFrom(methodDescriptor, anyMessage.value)
      }

    new InvocationContext(dynamicMessage, metadata)
  }
}
class InvocationContext(val message: DynamicMessage, val metadata: Metadata)
    extends DynamicMessageContext
    with MetadataContext
