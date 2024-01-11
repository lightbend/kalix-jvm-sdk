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

import java.lang.reflect.AnnotatedParameterizedType
import java.lang.reflect.Field
import java.time.Instant

import scala.jdk.CollectionConverters._

import com.fasterxml.jackson.dataformat.protobuf.ProtobufMapper
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufField
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufMessage
import com.fasterxml.jackson.dataformat.protobuf.{ schema => jacksonSchema }
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto

/**
 * Extracts a protobuf schema for a message, used only for assigning a typed schema to view state and results
 */
object ProtoMessageDescriptors {
  private val protobufMapper = ProtobufMapper.builder.addModule(new JavaTimeModule).build;

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

  private def isTimestampField(
      jacksonType: ProtobufMessage,
      protoField: ProtobufField,
      javaClass: Class[_]): Boolean = {

    def lookupOriginalJavaType(jacksonType: ProtobufMessage, javaClass: Class[_]): Option[Class[_]] = {

      def isClassMatch(cls: Class[_]): Boolean = cls.getSimpleName == jacksonType.getName
      def isFieldMatch(field: Field): Boolean = isClassMatch(field.getType)

      if (isClassMatch(javaClass)) Some(javaClass)
      else {
        javaClass.getDeclaredFields
          .collectFirst {
            // if current class has a field matching jacksonType,
            // we can already stop search and return this type
            case field if isFieldMatch(field) => field.getType
          }
          .orElse {
            // if current class doesn't have a matching field,
            // we need to scan its underling fields

            // we are only interested in fields for types that can potentially
            // have a field of type Instant, therefore we use the filter below will reduce the search space.
            val filterOutNonApplicable = (field: Field) => {
              val typ = field.getType
              typ != classOf[Instant] && // certainly not a type we want to scan in
              typ != javaClass && // eliminate recursive types (types having itself as fields)
              !typ.getPackageName.startsWith("java.lang") // exclude all 'java.lang' types
            }

            javaClass.getDeclaredFields
              .filter(filterOutNonApplicable)
              .collect {
                // for repeated fields (collections), we need the type parameter instead
                case field if field.getType.getPackageName.startsWith("java.util") =>
                  field.getAnnotatedType
                    .asInstanceOf[AnnotatedParameterizedType]
                    .getAnnotatedActualTypeArguments
                    .head // repeated fields are collections of one type param
                    .getType
                    .asInstanceOf[Class[_]]
                case field if field.getType.isArray => field.getType.getComponentType
                case field                          => field.getType
              }
              .flatMap(typ => lookupOriginalJavaType(jacksonType, typ))
              .headOption
          }
      }
    }

    if (!protoField.isObject && protoField.`type`.name() == "DOUBLE") {
      val originalJavaClass = lookupOriginalJavaType(jacksonType, javaClass)
      originalJavaClass match {
        case Some(cls) =>
          val instantField = cls.getDeclaredFields.find { field =>
            field.getName == protoField.name && field.getType == classOf[Instant]
          }
          instantField.nonEmpty
        case _ => false
      }
    } else {
      false
    }
  }

  private def toProto(jacksonType: ProtobufMessage, javaClass: Class[_]): DescriptorProtos.DescriptorProto = {

    val builder = DescriptorProtos.DescriptorProto.newBuilder()
    builder.setName(jacksonType.getName)
    jacksonType.fields().forEach { field =>
      val fieldDescriptor = DescriptorProtos.FieldDescriptorProto
        .newBuilder()
        .setName(field.name)
        .setNumber(field.id)

      if (isTimestampField(jacksonType, field, javaClass)) {
        fieldDescriptor.setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE)
        fieldDescriptor.setTypeName("google.protobuf.Timestamp")
      } else if (!field.isObject) {
        fieldDescriptor.setType(protoTypeFor(field))
      } else {
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
