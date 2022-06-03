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

import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType

import scala.jdk.CollectionConverters.IterableHasAsJava
import scala.jdk.CollectionConverters.IterableHasAsScala

import com.fasterxml.jackson.dataformat.protobuf.ProtobufMapper
import com.fasterxml.jackson.dataformat.protobuf.schema.FieldType
import com.fasterxml.jackson.dataformat.protobuf.schema.FieldType._
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema
import com.google.api.HttpRule
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.DescriptorProtos.MethodOptions
import com.google.protobuf.Descriptors
import kalix.javasdk.action.Action
import kalix.javasdk.valueentity.ValueEntity
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod

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
    override def allMessageTypes: Set[Class[_]] = Set(outputType, inputType)
  }

  case class ValueEntityTypesSelector(method: Method) extends TypesSelector {
    // gRPC input is the second param
    override val inputType: Class[_] = method.getParameterTypes()(1)
    override val outputType: Class[_] = returnParamType(method)
    override def allMessageTypes: Set[Class[_]] = Set(outputType, inputType, method.getParameterTypes()(0))
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

      // FIXME: still need to find out how is this structured.
      // I guess we will need to traverse the type looking for nested types (non scalar types)
      val fields = protobufSchema.getRootType.fields().asScala.map { field =>

        val builder =
          DescriptorProtos.FieldDescriptorProto.newBuilder
            .setName(field.name)
            .setNumber(field.id)
            .setType(mapScalarType(field.`type`))

        builder.build()
      }

      DescriptorProtos.DescriptorProto.newBuilder
        .setName(protobufSchema.getRootType.getName)
        .addAllField(fields.asJava)
        .build()
    }

    private def buildMethodHttpOptions(component: Class[_], method: Method): Option[MethodOptions] = {

      // TODO: also read class level mappings
      val ruleBuilder = HttpRule.newBuilder()

      Option(AnnotatedElementUtils.findMergedAnnotation(method, classOf[RequestMapping]))
        .map { mapping =>
          // TODO: just flatting the mapping for now, will need validate later
          val fullPath = mapping.path().mkString("/")

          // FIXME: naively picking the first only for now
          val httpMethod = mapping.method()(0)
          httpMethod match {
            case RequestMethod.GET   => ruleBuilder.setGet(fullPath)
            case RequestMethod.POST  => ruleBuilder.setPost(fullPath)
            case RequestMethod.PUT   => ruleBuilder.setPut(fullPath)
            case RequestMethod.PATCH => ruleBuilder.setPatch(fullPath)

            // TODO: to implement when we support ValueEntity / Views deletion
            case RequestMethod.DELETE =>
              throw new IllegalArgumentException(s"Unsupported Http method: $httpMethod")

            // those below are not supported in by the proto http annotations
            case RequestMethod.OPTIONS | RequestMethod.HEAD | RequestMethod.TRACE =>
              throw new IllegalArgumentException(s"Unsupported Http method: $httpMethod")
          }

          // FIXME: is that correct to always use '*'?
          val rule = ruleBuilder.setBody("*").build()
          MethodOptions
            .newBuilder()
            .setExtension(com.google.api.AnnotationsProto.http, rule)
            .build()
        }

    }

    def genFileDescriptor(
        component: Class[_],
        methods: Seq[Method],
        typesSelector: Method => TypesSelector): Descriptors.FileDescriptor = {

      val name = component.getSimpleName
      val packageName = component.getPackageName
      val protoMapper = new ProtobufMapper

      val protoBuilder = DescriptorProtos.FileDescriptorProto.newBuilder
      protoBuilder
        .setName(name + ".proto") // FIXME: snake_case this ?!
        .setSyntax("proto3")
        .setPackage(packageName)
        .setOptions(DescriptorProtos.FileOptions.newBuilder.setJavaMultipleFiles(true).build)

      // build messages types
      methods
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

      methods.foreach { method =>
        val methodBuilder = DescriptorProtos.MethodDescriptorProto.newBuilder

        val selector = typesSelector(method)
        val input = selector.inputType.getName
        val output = selector.outputType.getName

        methodBuilder
          .setName(method.getName.capitalize)
          .setInputType(input)
          .setOutputType(output)

        buildMethodHttpOptions(component, method).foreach { methodOptions =>
          methodBuilder.setOptions(methodOptions)
        }

        serviceBuilder.addMethod(methodBuilder.build())
      }

      protoBuilder.addService(serviceBuilder.build())

      // finally build all final descriptor
      Descriptors.FileDescriptor.buildFrom(protoBuilder.build, new Array[Descriptors.FileDescriptor](0))
    }
  }

  def generateFileDescriptorAction(component: Class[_ <: Action]): Descriptors.FileDescriptor = {
    val methods =
      component.getDeclaredMethods
        .filter(_.getReturnType == classOf[Action.Effect[_]])
        // actions have only one input param, always
        .filter(_.getParameters.length == 1)

    InternalGenerator.genFileDescriptor(component, methods, method => ActionTypesSelector(method))
  }

  def generateFileDescriptorValueEntity(component: Class[_ <: ValueEntity[_]]): Descriptors.FileDescriptor = {
    // look up state type
    val stateType = component.getDeclaredMethod("emptyState").getReturnType

    val methods =
      component.getDeclaredMethods
        .filter(_.getReturnType == classOf[ValueEntity.Effect[_]])
        // value entities have only two input param
        .filter { method =>
          method.getParameters.length == 2 &&
          method.getParameterTypes()(0) == stateType // first param must be the state
        }

    InternalGenerator.genFileDescriptor(component, methods, method => ValueEntityTypesSelector(method))
  }

}
