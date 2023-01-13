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

import akka.http.scaladsl.model.headers.CacheDirectives.public
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.{ Type => ProtoType }
import kalix.springsdk.testmodels.ComplexMessage
import kalix.springsdk.testmodels.TimeEnum
import kalix.springsdk.testmodels.{ NestedMessage, SimpleMessage }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.sql.Timestamp
import java.time.Instant

class MessageDescriptorGeneratorSpec extends AnyWordSpec with Matchers {

  "The message descriptor extraction" should {
    "create a schema for a simple message" in {
      val messageDescriptor = ProtoMessageDescriptors.generateMessageDescriptors(classOf[SimpleMessage])
      val descriptor = messageDescriptor.mainMessageDescriptor
      descriptor.getName shouldBe "SimpleMessage"
      // as ordered in message
      val expectedFieldsAndTypesInOrder = Seq(
        "c" -> ProtoType.TYPE_STRING,
        "by" -> ProtoType.TYPE_INT32,
        "n" -> ProtoType.TYPE_INT32,
        "l" -> ProtoType.TYPE_INT64,
        "d" -> ProtoType.TYPE_DOUBLE,
        "f" -> ProtoType.TYPE_FLOAT,
        "bo" -> ProtoType.TYPE_BOOL,
        // boxed primitives
        "cO" -> ProtoType.TYPE_STRING,
        "bO" -> ProtoType.TYPE_INT32,
        "nO" -> ProtoType.TYPE_INT32,
        "lO" -> ProtoType.TYPE_INT64,
        "dO" -> ProtoType.TYPE_DOUBLE,
        "fO" -> ProtoType.TYPE_FLOAT,
        "boO" -> ProtoType.TYPE_BOOL,
        // common object types mapping to proto primitives
        "s" -> ProtoType.TYPE_STRING,
        // arrays
        "iA" -> ProtoType.TYPE_INT32,
        "sA" -> ProtoType.TYPE_STRING)

      expectedFieldsAndTypesInOrder.zipWithIndex.foreach { case ((fieldName, protoType), index) =>
        withClue(s"index $index") {
          val field = descriptor.getField(index)
          field.getNumber shouldBe (index + 1) // field numbering start at 1
          field.getName shouldBe fieldName
          field.getType shouldBe protoType
        }
      }
    }

    "create a schema for a message with nested objects" in {
      val messageDescriptor = ProtoMessageDescriptors.generateMessageDescriptors(classOf[NestedMessage])
      val descriptor = messageDescriptor.mainMessageDescriptor
      descriptor.getName shouldBe "NestedMessage"
      val field1 = descriptor.getField(0) // note: not field number but 0 based index
      field1.getName shouldBe "one"
      val field2 = descriptor.getField(1)
      field2.getName shouldBe "two"
      field2.getType shouldBe ProtoType.TYPE_MESSAGE
      // nested message field type?

      messageDescriptor.additionalMessageDescriptors should have size 1
      messageDescriptor.additionalMessageDescriptors.head.getName shouldBe "SimpleMessage"
    }

    /* "create a schema for a message with Java Instant and enum doesn't break" in {
      ProtoMessageDescriptors.generateMessageDescriptors(classOf[TimeEnum])
    }*/

    "create a schema for a message with Java Timestamp doesn't break" in {

      val messageDescriptor = ProtoMessageDescriptors.generateMessageDescriptors(classOf[ComplexMessage])
      val descriptor = messageDescriptor.mainMessageDescriptor
      val field1 = descriptor.getField(0) // note: not field number but 0 based index
      field1.getName shouldBe "value"
      val field2 = descriptor.getField(1)
      field2.getName shouldBe "ts"
      field2.getType shouldBe ProtoType.TYPE_MESSAGE
      field2.getTypeName shouldBe "google.protobuf.Timestamp"
      val field3 = descriptor.getField(2)
      field3.getName shouldBe "msg"
      field3.getType shouldBe ProtoType.TYPE_MESSAGE
      field3.getTypeName shouldBe "InnerMessage"
    }

  }

}
