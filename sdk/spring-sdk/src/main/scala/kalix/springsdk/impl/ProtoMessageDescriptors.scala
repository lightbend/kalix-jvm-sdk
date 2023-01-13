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

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.`type`.TypeFactory
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.databind.module.SimpleModule
import com.google.protobuf.DescriptorProtos
import com.fasterxml.jackson.dataformat.protobuf.ProtobufMapper
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufMessage
import com.fasterxml.jackson.dataformat.protobuf.{ schema => jacksonSchema }
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto
import com.google.protobuf.util.Timestamps

import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.sql.Timestamp
import java.time.Instant
import java.util
import scala.jdk.CollectionConverters._
import scala.util.Try

/**
 * Extracts a protobuf schema for a message, used only for assigning a typed schema to view state and results
 */
object ProtoMessageDescriptors {

  class InstantSerializer extends JsonSerializer[Instant] {
    override def serialize(instant: Instant, gen: JsonGenerator, serializers: SerializerProvider): Unit = {
      val protoTimestamp = com.google.protobuf.Timestamp
        .newBuilder()
        .setSeconds(instant.getEpochSecond)
        .setNanos(instant.getNano)
        .build()

      gen.writeObject(protoTimestamp)
    }

    override def acceptJsonFormatVisitor(visitor: JsonFormatVisitorWrapper, `type`: JavaType): Unit = {
      visitor.expectObjectFormat(`type`)
    }
  }

  class InstantDeserializer extends JsonDeserializer[Instant] {
    override def deserialize(p: JsonParser, ctxt: DeserializationContext): Instant = {
      val protoTimestamp = p.readValueAs(classOf[com.google.protobuf.Timestamp])
      Instant.ofEpochSecond(protoTimestamp.getSeconds, protoTimestamp.getNanos)
    }
  }

  val tsModule = new SimpleModule()
  tsModule.addSerializer(classOf[Instant], new InstantSerializer())
  tsModule.addDeserializer(classOf[Instant], new InstantDeserializer())

  private val protobufMapper = ProtobufMapper.builder
    .addModule(new JavaTimeModule)
    //.addModule(tsModule)
    .build()

  def generateMessageDescriptors(javaClass: Class[_]): ProtoMessageDescriptors = {
    val jacksonProtoSchema = protobufMapper.generateSchemaFor(javaClass)

    val messages = jacksonProtoSchema.getMessageTypes.asScala.toSeq.map { messageType =>
      val jacksonType = jacksonProtoSchema.withRootType(messageType).getRootType
      toProto(jacksonType, javaClass)
    }
    val (Seq(mainDescriptor), otherDescriptors) =
      messages.partition(_.getName.endsWith(jacksonProtoSchema.getRootType.getName))
    ProtoMessageDescriptors(mainDescriptor, otherDescriptors)
  }

  private def toProto(jacksonType: ProtobufMessage, javaClass: Class[_]): DescriptorProtos.DescriptorProto = {
    val builder = DescriptorProtos.DescriptorProto.newBuilder()
    builder.setName(jacksonType.getName)
    jacksonType.fields().forEach { field =>
      val fieldDescriptor = DescriptorProtos.FieldDescriptorProto
        .newBuilder()
        .setName(field.name)
        .setNumber(field.id)

      /*if (Try(javaClass.getField(field.name))
          .map(_.getAnnotatedType.getType)
          .getOrElse(classOf[Nothing])
          .equals(classOf[Instant]) ||
        Try(javaClass.getField(field.name))
          .map(_.getAnnotatedType.getType)
          .getOrElse(classOf[Nothing])
          .equals(classOf[Timestamp]) ||
        (javaClass.isRecord && javaClass.getRecordComponents
          .find(_.getName == field.name)
          .map(_.getAnnotatedType.getType)
          .getOrElse(classOf[Nothing])
          .equals(classOf[Instant])) ||
        (classOf[util.Collection[_]].isAssignableFrom(javaClass) &&
        Try(
          javaClass.getGenericSuperclass
            .asInstanceOf[ParameterizedType]
            .getActualTypeArguments
            .head
            .getClass
            .getField(field.name))
          .map(_.getAnnotatedType.getType)
          .getOrElse(classOf[Nothing])
          .equals(classOf[Instant]))) {
        fieldDescriptor.setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE)
        fieldDescriptor.setTypeName("google.protobuf.Timestamp")
      } else */
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
      case "ENUM"       => DescriptorProtos.FieldDescriptorProto.Type.TYPE_ENUM
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
