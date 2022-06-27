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

import java.lang.reflect.Type
import scala.reflect.ClassTag
import com.google.api.AnnotationsProto
import com.google.api.CustomHttpPattern
import com.google.api.HttpRule
import com.google.protobuf.ByteString
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.DescriptorProtos.DescriptorProto
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto
import com.google.protobuf.DescriptorProtos.MethodOptions
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto
import com.google.protobuf.Descriptors
import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.{ Any => JavaPbAny }
import kalix.springsdk.annotations.Entity
import kalix.springsdk.annotations.Table
import kalix.springsdk.impl.reflection.{
  AnyServiceMethod,
  DynamicMessageContext,
  ExtractorCreator,
  KalixMethod,
  NameGenerator,
  ParameterExtractor,
  ParameterExtractors,
  ServiceMethod,
  SpringRestServiceMethod
}
import kalix.springsdk.impl.reflection.ParameterExtractors.HeaderExtractor
import kalix.springsdk.impl.reflection.RestServiceIntrospector.BodyParameter
import kalix.springsdk.impl.reflection.RestServiceIntrospector.HeaderParameter
import kalix.springsdk.impl.reflection.RestServiceIntrospector.PathParameter
import kalix.springsdk.impl.reflection.RestServiceIntrospector.QueryParamParameter
import kalix.springsdk.impl.reflection.RestServiceIntrospector.UnhandledParameter
import org.springframework.web.bind.annotation.RequestMethod

object ComponentDescriptor {
  def descriptorFor[T](implicit ev: ClassTag[T]): ComponentDescriptor =
    descriptorFor(ev.runtimeClass)

  def descriptorFor[T](component: Class[T]): ComponentDescriptor =
    getFactory(component).buildDescriptor(new NameGenerator)

  // TODO: add more validations here
  // we should let users know if components are missing required annotations,
  // eg: entities require @Entity, view require @Table and @Subscription
  private def getFactory[T](component: Class[T]): ComponentDescriptorFactory[T] = {
    if (component.getAnnotation(classOf[Entity]) != null)
      EntityDescriptorFactory(component)
    else if (component.getAnnotation(classOf[Table]) != null)
      ViewDescriptorFactory(component)
    else
      ActionDescriptorFactory(component)
  }

}

class ComponentDescriptor(serviceName: String, packageName: String, nameGenerator: NameGenerator) {

  private val grpcService = ServiceDescriptorProto.newBuilder()
  grpcService.setName(serviceName)

  private var inputMessageProtos: Seq[DescriptorProtos.DescriptorProto] = Seq.empty
  private var componentMethods: Seq[NamedComponentMethod] = Seq.empty

  // intermediate format that references input message by name
  // once we have built the full file descriptor, we can look up for the input message using its name
  case class NamedComponentMethod(
      serviceMethod: ServiceMethod,
      grpcMethodName: String,
      extractorCreators: Map[Int, ExtractorCreator],
      inputMessageName: String) {

    type ParameterExtractors = Array[ParameterExtractor[InvocationContext, AnyRef]]

    def toComponentMethod(fileDescriptor: FileDescriptor): ComponentMethod = {
      serviceMethod match {
        case method: SpringRestServiceMethod =>
          val message = fileDescriptor.findMessageTypeByName(inputMessageName)

          val parameterExtractors: ParameterExtractors =
            method.params.zipWithIndex.map { case (param, idx) =>
              extractorCreators.find(_._1 == idx) match {
                case Some((_, creator)) =>
                  creator(message)
                case None =>
                  // Yet to resolve this parameter, resolve now
                  param match {
                    case hp: HeaderParameter =>
                      new HeaderExtractor[AnyRef](hp.name, identity)
                    case UnhandledParameter(param) =>
                      throw new RuntimeException("Unhandled parameter: " + param)
                  }
              }
            }.toArray

          ComponentMethod(serviceMethod.javaMethodOpt, grpcMethodName, parameterExtractors, message)

        case method: AnyServiceMethod =>
          // methods that receive Any as input always default to AnyBodyExtractor
          val parameterExtractors: ParameterExtractors = Array(
            new ParameterExtractors.AnyBodyExtractor(method.inputType))
          ComponentMethod(serviceMethod.javaMethodOpt, grpcMethodName, parameterExtractors, JavaPbAny.getDescriptor)
      }

    }
  }

  def withMethod(kalixMethod: KalixMethod) = {

    val httpRuleBuilder = buildHttpRule(kalixMethod.serviceMethod)

    val (inputMessageName, extractors) =
      kalixMethod.serviceMethod match {
        case serviceMethod: SpringRestServiceMethod =>
          val (inputProto, extractors) =
            buildSyntheticMessageAndExtractors(serviceMethod, httpRuleBuilder, kalixMethod.entityKeys)
          inputMessageProtos = inputMessageProtos :+ inputProto
          (inputProto.getName, extractors)

        case _: AnyServiceMethod =>
          (JavaPbAny.getDescriptor.getFullName, Map.empty[Int, ExtractorCreator])
      }

    val grpcMethodName = nameGenerator.getName(kalixMethod.serviceMethod.methodName.capitalize)
    val grpcMethodBuilder = buildGrpcMethod(grpcMethodName, inputMessageName)

    val methodOptions =
      kalixMethod.methodOptions.foldLeft(MethodOptions.newBuilder()) { (optionsBuilder, options) =>
        optionsBuilder.setExtension(kalix.Annotations.method, options)
      }

    methodOptions.setExtension(AnnotationsProto.http, httpRuleBuilder.build())

    grpcMethodBuilder.setOptions(methodOptions.build())

    val grpcMethod = grpcMethodBuilder.build()
    grpcService.addMethod(grpcMethod)

    val componentMethod =
      NamedComponentMethod(kalixMethod.serviceMethod, grpcMethodName, extractors, inputMessageName)

    componentMethods = componentMethods :+ componentMethod

    this
  }

  def serviceDescriptor: Descriptors.ServiceDescriptor =
    fileDescriptor.findServiceByName(grpcService.getName)

  lazy val fileDescriptor: Descriptors.FileDescriptor =
    ProtoDescriptorGenerator.genFileDescriptor(serviceName, packageName, grpcService.build(), inputMessageProtos)

  lazy val methods: Map[String, ComponentMethod] =
    componentMethods.map { method => (method.grpcMethodName, method.toComponentMethod(fileDescriptor)) }.toMap

  private def buildSyntheticMessageAndExtractors(
      serviceMethod: SpringRestServiceMethod,
      httpRule: HttpRule.Builder,
      entityKeys: Seq[String] = Seq.empty): (DescriptorProto, Map[Int, ExtractorCreator]) = {

    val methodName = serviceMethod.methodName
    val inputMessageName = nameGenerator.getName(methodName + "Request").capitalize

    val inputMessageDescriptor = DescriptorProto.newBuilder()
    inputMessageDescriptor.setName(inputMessageName)

    def addEntityKeyIfNeeded(paramName: String, fieldDescriptor: FieldDescriptorProto.Builder) =
      if (entityKeys.contains(paramName)) {
        val fieldOptions = kalix.FieldOptions.newBuilder().setEntityKey(true).build()
        val options =
          DescriptorProtos.FieldOptions
            .newBuilder()
            .setExtension(kalix.Annotations.field, fieldOptions)
            .build()

        fieldDescriptor.setOptions(options)
      }

    val indexedParams = serviceMethod.params.zipWithIndex
    val bodyField = indexedParams.collectFirst { case (BodyParameter(param, _), idx) =>
      httpRule.setBody("json_body")
      val fieldDescriptor = FieldDescriptorProto.newBuilder()
      // todo ensure this is unique among field names
      fieldDescriptor.setName("json_body")
      // Always put the body at position 1 - even if there's no body, leave position 1 free. This keeps the body
      // parameter stable in case the user adds a body.
      fieldDescriptor.setNumber(1)
      fieldDescriptor.setType(FieldDescriptorProto.Type.TYPE_MESSAGE)
      fieldDescriptor.setTypeName("google.protobuf.Any")
      inputMessageDescriptor.addField(fieldDescriptor)
      idx -> new ExtractorCreator {
        override def apply(descriptor: Descriptors.Descriptor): ParameterExtractor[DynamicMessageContext, AnyRef] = {
          new ParameterExtractors.BodyExtractor(descriptor.findFieldByNumber(1), param.getParameterType)
        }
      }
    }

    val pathParamFields = serviceMethod.parsedPath.fields.zipWithIndex.flatMap { case (paramName, fieldIdx) =>
      val (maybeParamIdx, paramType) = indexedParams
        .collectFirst {
          case (p: PathParameter, idx) if p.name == paramName =>
            Some(idx) -> p.param.getGenericParameterType
        }
        .getOrElse(None -> classOf[String])

      val fieldDescriptor = FieldDescriptorProto.newBuilder()
      fieldDescriptor.setName(paramName)
      val fieldNumber = fieldIdx + 2
      fieldDescriptor.setNumber(fieldNumber)
      fieldDescriptor.setType(mapJavaTypeToProtobuf(paramType))
      addEntityKeyIfNeeded(paramName, fieldDescriptor)
      inputMessageDescriptor.addField(fieldDescriptor)
      maybeParamIdx.map(_ -> new ExtractorCreator {
        override def apply(descriptor: Descriptors.Descriptor): ParameterExtractor[DynamicMessageContext, AnyRef] = {
          new ParameterExtractors.FieldExtractor[AnyRef](descriptor.findFieldByNumber(fieldNumber), identity)
        }
      })
    }

    val queryFieldsOffset = pathParamFields.size + 2

    val queryFields = indexedParams
      .collect { case (qp: QueryParamParameter, idx) =>
        idx -> qp
      }
      .sortBy(_._2.name)
      .zipWithIndex
      .map { case ((paramIdx, param), fieldIdx) =>
        val fieldNumber = fieldIdx + queryFieldsOffset
        val fieldDescriptor = FieldDescriptorProto.newBuilder()
        fieldDescriptor.setName(param.name)
        fieldDescriptor.setNumber(fieldNumber)
        fieldDescriptor.setType(mapJavaTypeToProtobuf(param.param.getGenericParameterType))
        inputMessageDescriptor.addField(fieldDescriptor)
        addEntityKeyIfNeeded(param.name, fieldDescriptor)
        paramIdx -> new ExtractorCreator {
          override def apply(descriptor: Descriptors.Descriptor): ParameterExtractor[DynamicMessageContext, AnyRef] = {
            new ParameterExtractors.FieldExtractor[AnyRef](descriptor.findFieldByNumber(fieldNumber), identity)
          }
        }
      }

    val inputProto = inputMessageDescriptor.build()
    val extractors = (bodyField.toSeq ++ pathParamFields ++ queryFields).toMap
    (inputProto, extractors)
  }

  private def mapJavaTypeToProtobuf(javaType: Type): FieldDescriptorProto.Type = {
    // todo make this smarter, eg, customizable parameter deserializers, UUIDs, byte arrays, enums etc
    if (javaType == classOf[String]) {
      FieldDescriptorProto.Type.TYPE_STRING
    } else if (javaType == classOf[java.lang.Long]) {
      FieldDescriptorProto.Type.TYPE_INT64
    } else if (javaType == classOf[java.lang.Integer]) {
      FieldDescriptorProto.Type.TYPE_INT32
    } else if (javaType == classOf[java.lang.Double]) {
      FieldDescriptorProto.Type.TYPE_DOUBLE
    } else if (javaType == classOf[java.lang.Float]) {
      FieldDescriptorProto.Type.TYPE_FLOAT
    } else if (javaType == classOf[java.lang.Boolean]) {
      FieldDescriptorProto.Type.TYPE_BOOL
    } else if (javaType == classOf[ByteString]) {
      FieldDescriptorProto.Type.TYPE_BYTES
    } else {
      throw new RuntimeException("Don't know how to extract type " + javaType + " from path.")
    }
  }

  private def buildHttpRule(serviceMethod: ServiceMethod) = {
    val httpRule = HttpRule.newBuilder()
    val pathTemplate = serviceMethod.pathTemplate
    serviceMethod.requestMethod match {
      case RequestMethod.GET =>
        httpRule.setGet(pathTemplate)
      case RequestMethod.POST =>
        httpRule.setPost(pathTemplate)
      case RequestMethod.PUT =>
        httpRule.setPut(pathTemplate)
      case RequestMethod.PATCH =>
        httpRule.setPatch(pathTemplate)
      case RequestMethod.DELETE =>
        httpRule.setDelete(pathTemplate)
      case other =>
        httpRule.setCustom(
          CustomHttpPattern
            .newBuilder()
            .setKind(other.name())
            .setPath(pathTemplate))
    }
    httpRule
  }

  private def buildGrpcMethod(grpcMethodName: String, inputTypeName: String): MethodDescriptorProto.Builder =
    MethodDescriptorProto
      .newBuilder()
      .setName(grpcMethodName)
      .setInputType(inputTypeName)
      .setOutputType("google.protobuf.Any")

}
