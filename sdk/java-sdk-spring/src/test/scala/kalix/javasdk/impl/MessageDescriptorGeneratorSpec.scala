/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl

import com.google.protobuf.DescriptorProtos
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.{ Type => ProtoType }
import kalix.spring.testmodels.NestedMessage
import kalix.spring.testmodels.SimpleMessage
import kalix.spring.testmodels.TimeEnum
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MessageDescriptorGeneratorSpec extends AnyWordSpec with Matchers {

  "The message descriptor extraction" should {
    "create a schema for a simple message" in {
      val messageDescriptor = ProtoMessageDescriptors.generateMessageDescriptors(classOf[SimpleMessage])
      val descriptor: DescriptorProtos.DescriptorProto = messageDescriptor.mainMessageDescriptor
      descriptor.getName shouldBe "SimpleMessage"
      checkSimpleMessageFields(descriptor)
    }

    "create a schema for a message with nested objects" in {
      val messageDescriptor = ProtoMessageDescriptors.generateMessageDescriptors(classOf[NestedMessage])
      val descriptor = messageDescriptor.mainMessageDescriptor
      descriptor.getName shouldBe "NestedMessage"

      val field0 = descriptor.getField(0) // note: not field number but 0 based index
      field0.getName shouldBe "string"

      val field1 = descriptor.getField(1)
      field1.getName shouldBe "simpleMessage"
      field1.getType shouldBe ProtoType.TYPE_MESSAGE

      val field2 = descriptor.getField(2)
      field2.getName shouldBe "instantWrapper"
      field2.getType shouldBe ProtoType.TYPE_MESSAGE

      val field3 = descriptor.getField(3)
      field3.getName shouldBe "instantsList"
      field3.getType shouldBe ProtoType.TYPE_MESSAGE
      field3.getLabel shouldBe Label.LABEL_REPEATED

      val field4 = descriptor.getField(4)
      field4.getName shouldBe "instantArrays"
      field4.getType shouldBe ProtoType.TYPE_MESSAGE
      field4.getLabel shouldBe Label.LABEL_REPEATED

      messageDescriptor.additionalMessageDescriptors should have size 4

      val instantWrapper =
        messageDescriptor.additionalMessageDescriptors.find(_.getName == "InstantWrapper").get
      instantWrapper.getField(0).getType shouldBe ProtoType.TYPE_MESSAGE
      instantWrapper.getField(0).getTypeName shouldBe "google.protobuf.Timestamp"

      val instantEntryForList =
        messageDescriptor.additionalMessageDescriptors.find(_.getName == "InstantEntryForList").get
      instantEntryForList.getField(0).getType shouldBe ProtoType.TYPE_MESSAGE
      instantEntryForList.getField(0).getTypeName shouldBe "google.protobuf.Timestamp"

      val instantEntryForArray =
        messageDescriptor.additionalMessageDescriptors.find(_.getName == "InstantEntryForArray").get
      instantEntryForArray.getField(0).getType shouldBe ProtoType.TYPE_MESSAGE
      instantEntryForArray.getField(0).getTypeName shouldBe "google.protobuf.Timestamp"

    }

    "create a schema for a message with Java Instant and enum doesn't break" in {
      val messageDescriptor = ProtoMessageDescriptors.generateMessageDescriptors(classOf[TimeEnum])
      val descriptor: DescriptorProtos.DescriptorProto = messageDescriptor.mainMessageDescriptor
      descriptor.getName shouldBe "TimeEnum"

      val field0 = descriptor.getField(0)
      field0.getName shouldBe "time"
      field0.getType shouldBe ProtoType.TYPE_MESSAGE
      field0.getTypeName shouldBe "google.protobuf.Timestamp"

      val field1 = descriptor.getField(1)
      field1.getName shouldBe "lev"
      field1.getType shouldBe ProtoType.TYPE_ENUM
    }

  }

  private def checkSimpleMessageFields(descriptor: DescriptorProtos.DescriptorProto): Unit = {
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
      "sA" -> ProtoType.TYPE_STRING,
      // Instant mapping to message
      "inst" -> ProtoType.TYPE_MESSAGE)

    expectedFieldsAndTypesInOrder.zipWithIndex.foreach { case ((fieldName, protoType), index) =>
      withClue(s"index $index") {
        val field = descriptor.getField(index)
        field.getNumber shouldBe (index + 1) // field numbering start at 1
        field.getName shouldBe fieldName
        field.getType shouldBe protoType
      }
    }
  }

}
