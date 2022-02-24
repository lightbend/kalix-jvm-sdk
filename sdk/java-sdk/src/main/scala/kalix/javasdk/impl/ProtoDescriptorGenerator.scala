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

package kalix.javasdk.impl

import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType

import scala.jdk.CollectionConverters.IterableHasAsJava
import scala.jdk.CollectionConverters.IterableHasAsScala

import kalix.javasdk.EntityId
import kalix.javasdk.action.Action
import kalix.javasdk.valueentity.ValueEntity
import com.fasterxml.jackson.dataformat.protobuf.ProtobufMapper
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.Descriptors
import com.fasterxml.jackson.dataformat.protobuf.schema.FieldType
import com.fasterxml.jackson.dataformat.protobuf.schema.FieldType._

object ProtoDescriptorGenerator {

  sealed trait TypesSelector {
    def returnParamTypeName(method: Method) = {
      val genericReturnType = method.getGenericReturnType.asInstanceOf[ParameterizedType]
      genericReturnType.getActualTypeArguments()(0).getTypeName
    }

    def returnParamType(method: Method) =
      try {
        // FIXME: don't load class, but extract the simple name using regex
        Class.forName(returnParamTypeName(method))
      } catch {
        case e: ClassNotFoundException =>
          throw new RuntimeException(e)
      }

    def inputType: Class[_]
    def outputType: Class[_]
    def allMessageTypes: Set[Class[_]]
  }

  case class ActionTypesSelector(method: Method) extends TypesSelector {
    override val inputType: Class[_] = method.getParameterTypes()(0)
    override val outputType: Class[_] = returnParamType(method)
    override def allMessageTypes: Set[Class[_]] =
      Set(outputType, inputType)
  }

  case class ValueEntityTypesSelector(method: Method) extends TypesSelector {
    // gRPC input is the second param
    override val inputType: Class[_] = method.getParameterTypes()(1)
    override val outputType: Class[_] = returnParamType(method)

    override def allMessageTypes: Set[Class[_]] =
      Set(outputType, inputType, method.getParameterTypes()(0))
  }

  private object InternalGenerator {

    private def mapScalarType(fieldType: FieldType): DescriptorProtos.FieldDescriptorProto.Type =
      fieldType match {
        case STRING     => DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING
        case MESSAGE    => DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE
        case VINT64_STD => DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64
        case VINT32_STD => DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32
        case BOOLEAN    => DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL
        case DOUBLE     => DescriptorProtos.FieldDescriptorProto.Type.TYPE_DOUBLE
        case FLOAT      => DescriptorProtos.FieldDescriptorProto.Type.TYPE_FLOAT
        case BYTES      => DescriptorProtos.FieldDescriptorProto.Type.TYPE_BYTES
        // FIXME: non-exhaustive - map them all!!
        case other => throw new IllegalArgumentException(s"Unknown field type $other")
      }

    private def buildMessageType(
        protobufSchema: ProtobufSchema,
        messageType: Class[_]): DescriptorProtos.DescriptorProto = {

      def entityIdFieldName(messageType: Class[_]): Option[String] = {
        // FIXME: we will need more robust validation. Only one field should be annotated, for example?
        messageType.getFields.collectFirst {
          case field if field.getAnnotation(classOf[EntityId]) != null => field.getName
        }
      }

      // FIXME: still need to find out how is this structured.
      // I guess we will need to traverse the type looking for nested types (non scalar types)
      val fields = protobufSchema.getRootType.fields().asScala.map { field =>

        val builder =
          DescriptorProtos.FieldDescriptorProto.newBuilder
            .setName(field.name)
            .setNumber(field.id)
            .setType(mapScalarType(field.`type`))

        // FIXME: we will need the same for fields marked with JWT annotations
        if (entityIdFieldName(messageType).exists(_ == field.name)) {
          val fieldOptions = kalix.FieldOptions.newBuilder().setEntityKey(true).build()

          val options =
            DescriptorProtos.FieldOptions
              .newBuilder()
              .setExtension(kalix.Annotations.field, fieldOptions)
              .build()

          builder.setOptions(options)
        }

        builder.build()
      }

      DescriptorProtos.DescriptorProto.newBuilder
        .setName(protobufSchema.getRootType.getName)
        .addAllField(fields.asJava)
        .build()
    }

    def genFileDescriptor(
        name: String,
        packageName: String,
        handlers: Seq[Method],
        typesSelector: Method => TypesSelector): Descriptors.FileDescriptor = {

      val protoMapper = new ProtobufMapper

      val protoBuilder = DescriptorProtos.FileDescriptorProto.newBuilder
      protoBuilder
        .setName(name + ".proto") // FIXME: snake_case this ?!
        .setSyntax("proto3")
        .setPackage(packageName)
        .setOptions(DescriptorProtos.FileOptions.newBuilder.setJavaMultipleFiles(true).build)

      // build messages types
      handlers
        .flatMap(method => typesSelector(method).allMessageTypes)
        // types can be used many times, we need unique descriptors
        .toSet
        .foreach { messageType: Class[_] =>
          val schema = protoMapper.generateSchemaFor(messageType)
          protoBuilder.addMessageType(buildMessageType(schema, messageType))
        }

      // build gRPC service
      val serviceBuilder = DescriptorProtos.ServiceDescriptorProto.newBuilder
      serviceBuilder.setName(name)

      handlers.foreach { method =>
        val methodBuilder = DescriptorProtos.MethodDescriptorProto.newBuilder

        val selector = typesSelector(method)
        val input = selector.inputType.getName
        val output = selector.outputType.getName

        methodBuilder
          .setName(method.getName.capitalize)
          .setInputType(input)
          .setOutputType(output)

        serviceBuilder.addMethod(methodBuilder.build())
      }

      protoBuilder.addService(serviceBuilder.build())

      // finally build all final descriptor
      Descriptors.FileDescriptor.buildFrom(protoBuilder.build, new Array[Descriptors.FileDescriptor](0))
    }
  }

  def generateFileDescriptorAction(component: Class[_ <: Action]): Descriptors.FileDescriptor = {
    val handler =
      component.getDeclaredMethods
        .filter(_.getReturnType == classOf[Action.Effect[_]])
        // actions have only one input param, always
        .filter(_.getParameters.length == 1)

    InternalGenerator.genFileDescriptor(
      component.getSimpleName,
      component.getPackageName,
      handler,
      method => ActionTypesSelector(method))
  }

  def generateFileDescriptorValueEntity(component: Class[_ <: ValueEntity[_]]): Descriptors.FileDescriptor = {
    // look up state type
    val stateType = component.getDeclaredMethod("emptyState").getReturnType

    val handlers =
      component.getDeclaredMethods
        .filter(_.getReturnType == classOf[ValueEntity.Effect[_]])
        // value entities have only two input param
        .filter { method =>
          method.getParameters.length == 2 &&
          method.getParameterTypes()(0) == stateType // first param must be the state
        }

    InternalGenerator.genFileDescriptor(
      component.getSimpleName,
      component.getPackageName,
      handlers,
      method => ValueEntityTypesSelector(method))
  }

}
