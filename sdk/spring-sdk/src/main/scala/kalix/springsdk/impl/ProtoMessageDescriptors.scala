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

import com.google.protobuf.{ DescriptorProtos, Descriptors, FieldType }
import com.fasterxml.jackson.dataformat.protobuf.ProtobufMapper
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufMessage
import com.fasterxml.jackson.dataformat.protobuf.{ schema => jacksonSchema }
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto

import scala.jdk.CollectionConverters._

/**
 * Extracts a protobuf schema for a message, used only for assigning a typed schema to view state and results
 */
object ProtoMessageDescriptors {
  private val protobufMapper = new ProtobufMapper()

  def generateMessageDescriptors(javaClass: Class[_]): ProtoMessageDescriptors = {
    val jacksonProtoSchema = protobufMapper.generateSchemaFor(javaClass)

    val messages = jacksonProtoSchema.getMessageTypes.asScala.toSeq.map { messageType =>
      val jacksonType = jacksonProtoSchema.withRootType(messageType).getRootType
      toProto(jacksonType)
    }
    val (Seq(mainDescriptor), otherDescriptors) =
      messages.partition(_.getName.endsWith(jacksonProtoSchema.getRootType.getName))
    ProtoMessageDescriptors(mainDescriptor, otherDescriptors)
  }

  private def toProto(jacksonType: ProtobufMessage): DescriptorProtos.DescriptorProto = {
    val builder = DescriptorProtos.DescriptorProto.newBuilder()
    builder.setName(jacksonType.getName)
    jacksonType.fields().forEach { field =>
      val fieldDescriptor = DescriptorProtos.FieldDescriptorProto
        .newBuilder()
        .setName(field.name)
        .setNumber(field.id)

      if (!field.isObject)
        fieldDescriptor.setType(protoTypeFor(field))
      else {
        fieldDescriptor.setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE)
        fieldDescriptor.setTypeName(field.getMessageType.getName)
      }

      if (field.isArray)
        fieldDescriptor.setLabel(FieldDescriptorProto.Label.LABEL_REPEATED)

      builder.addField(fieldDescriptor)
    }
    builder.build()
  }

  private def protoTypeFor(t: jacksonSchema.ProtobufField): DescriptorProtos.FieldDescriptorProto.Type = {
    // jackson protobuf has its own type names, map those
    t.`type`.name() match {
      case "STRING"     => DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING
      case "BOOLEAN"    => DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL
      case "VINT32_STD" => DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32
      case "VINT64_STD" => DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64
      case "DOUBLE"     => DescriptorProtos.FieldDescriptorProto.Type.TYPE_DOUBLE
      case "FLOAT"      => DescriptorProtos.FieldDescriptorProto.Type.TYPE_FLOAT
      /* case "uint32"     => DescriptorProtos.FieldDescriptorProto.Type.TYPE_UINT32
      case "uint64"     => DescriptorProtos.FieldDescriptorProto.Type.TYPE_UINT64
      case "sint32"     => DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT32
      case "sint64"     => DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT64
      case "fixed32"    => DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED32
      case "fixed64"    => DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED64 */
      case message => throw new MatchError(s"No type for [$message] yet")
    }
  }

}

case class ProtoMessageDescriptors(
    mainMessageDescriptor: DescriptorProtos.DescriptorProto,
    additionalMessageDescriptors: Seq[DescriptorProtos.DescriptorProto])
