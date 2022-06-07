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

package kalix.springsdk.impl.reflection

import com.fasterxml.jackson.databind.{ ObjectMapper, ObjectReader }
import com.google.protobuf.{ Any, ByteString, Descriptors, DynamicMessage }
import kalix.javasdk.Metadata
import scala.jdk.OptionConverters._

/**
 * Extracts method parameters from an invocation context for the purpose of passing them to a reflective invocation call
 */
trait ParameterExtractor[-C, +T] {
  def extract(context: C): T
}

trait MetadataContext {
  def metadata: Metadata
}

trait DynamicMessageContext {
  def message: DynamicMessage
}

object ParameterExtractors {

  class BodyExtractor[T](field: Descriptors.FieldDescriptor, reader: ObjectReader)
      extends ParameterExtractor[DynamicMessageContext, T] {
    override def extract(context: DynamicMessageContext): T = {
      context.message.getField(field) match {
        case dm: DynamicMessage =>
          // todo validate type_url
          val bytes = dm.getField(Any.getDescriptor.findFieldByName("value")).asInstanceOf[ByteString]
          reader.readValue(bytes.newInput())
      }
    }
  }

  class FieldExtractor[T](field: Descriptors.FieldDescriptor, deserialize: AnyRef => T)
      extends ParameterExtractor[DynamicMessageContext, T] {
    override def extract(context: DynamicMessageContext): T = {
      deserialize(context.message.getField(field))
    }
  }

  class HeaderExtractor[T >: Null](name: String, deserialize: String => T)
      extends ParameterExtractor[MetadataContext, T] {
    override def extract(context: MetadataContext): T = context.metadata.get(name).toScala.map(deserialize).orNull
  }
}
