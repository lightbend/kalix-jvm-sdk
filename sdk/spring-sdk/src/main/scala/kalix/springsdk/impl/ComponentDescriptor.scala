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
import kalix.springsdk.impl.reflection.CombinedSubscriptionServiceMethod
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
import kalix.springsdk.impl.reflection.SubscriptionServiceMethod
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.RequestMethod

import java.lang.reflect.Method
import scala.jdk.CollectionConverters.CollectionHasAsScala

/**
 * The component descriptor is both used for generating the protobuf service descriptor to communicate the service type
 * and methods etc. to Kalix and for the reflective routers routing incoming calls to the right method of the user
 * component class.
 */
private[impl] object ComponentDescriptor {

  val logger = LoggerFactory.getLogger(ComponentMethod.getClass)

  def descriptorFor[T](implicit ev: ClassTag[T]): ComponentDescriptor =
    descriptorFor(ev.runtimeClass)

  def descriptorFor(component: Class[_]): ComponentDescriptor =
    ComponentDescriptorFactory.getFactoryFor(component).buildDescriptorFor(component, new NameGenerator)

  def apply(
      nameGenerator: NameGenerator,
      serviceName: String,
      packageName: String,
      serviceMethods: Seq[KalixMethod],
      additionalMessages: Seq[ProtoMessageDescriptors] = Nil): ComponentDescriptor = {
    val otherMessageProtos: Seq[DescriptorProtos.DescriptorProto] =
      additionalMessages.flatMap(pm => pm.mainMessageDescriptor +: pm.additionalMessageDescriptors)

    val grpcService = ServiceDescriptorProto.newBuilder()
    grpcService.setName(serviceName)

    def methodToNamedComponentMethod(kalixMethod: KalixMethod): NamedComponentMethod = {

      val httpRuleBuilder = buildHttpRule(kalixMethod.serviceMethod)

      val (inputMessageName: String, extractors: Map[Int, ExtractorCreator], inputProto: Option[DescriptorProto]) =
        kalixMethod.serviceMethod match {
          case serviceMethod: SpringRestServiceMethod =>
            val (inputProto, extractors) =
              buildSyntheticMessageAndExtractors(nameGenerator, serviceMethod, httpRuleBuilder, kalixMethod.entityKeys)
            (inputProto.getName, extractors, Some(inputProto))

          case _: AnyServiceMethod =>
            (JavaPbAny.getDescriptor.getFullName, Map.empty[Int, ExtractorCreator], None)
        }

      val grpcMethodName = nameGenerator.getName(kalixMethod.serviceMethod.methodName.capitalize)
      val grpcMethodBuilder =
        buildGrpcMethod(
          grpcMethodName,
          inputMessageName,
          kalixMethod.serviceMethod.streamIn,
          kalixMethod.serviceMethod.streamOut)

      val methodOptions = MethodOptions.newBuilder()
      kalixMethod.methodOptions.foreach(option => methodOptions.setExtension(kalix.Annotations.method, option))

      methodOptions.setExtension(AnnotationsProto.http, httpRuleBuilder.build())

      grpcMethodBuilder.setOptions(methodOptions.build())

      val grpcMethod = grpcMethodBuilder.build()
      grpcService.addMethod(grpcMethod)

      NamedComponentMethod(kalixMethod.serviceMethod, grpcMethodName, extractors, inputMessageName, inputProto, kalixMethod)
    }

    val namedMethods: Seq[NamedComponentMethod] = serviceMethods.map(methodToNamedComponentMethod)
    val inputMessageProtos: Seq[DescriptorProtos.DescriptorProto] = namedMethods.flatMap(_.inputProto)

    val fileDescriptor: Descriptors.FileDescriptor =
      ProtoDescriptorGenerator.genFileDescriptor(
        serviceName,
        packageName,
        grpcService.build(),
        inputMessageProtos ++ otherMessageProtos)

    val methods: Map[String, ComponentMethod] =
      namedMethods.map { method => (method.grpcMethodName, method.toComponentMethod(fileDescriptor)) }.toMap

    val serviceDescriptor: Descriptors.ServiceDescriptor =
      fileDescriptor.findServiceByName(grpcService.getName)

    new ComponentDescriptor(serviceName, packageName, methods, serviceDescriptor, fileDescriptor)
  }

  // intermediate format that references input message by name
  // once we have built the full file descriptor, we can look up for the input message using its name
  private case class NamedComponentMethod(
      serviceMethod: ServiceMethod,
      grpcMethodName: String,
      extractorCreators: Map[Int, ExtractorCreator],
      inputMessageName: String,
      inputProto: Option[DescriptorProto],
      kalixMethod: KalixMethod) {

    type ParameterExtractors = Array[ParameterExtractor[InvocationContext, AnyRef]]

    def toComponentMethod(fileDescriptor: FileDescriptor): ComponentMethod = {
      serviceMethod match {
        case method: SpringRestServiceMethod =>
          val message = fileDescriptor.findMessageTypeByName(inputMessageName)
          if (message == null)
            throw new RuntimeException(
              "Unknown message type [" + inputMessageName + "], known are [" + fileDescriptor.getMessageTypes.asScala
                .map(_.getName) + "]")

          val parameterExtractors: ParameterExtractors =
            if (method.callable) {
              method.params.zipWithIndex.map { case (param, idx) =>
                extractorCreators.find(_._1 == idx) match {
                  case Some((_, creator)) =>
                    creator(message)
                  case None =>
                    // Yet to resolve this parameter, resolve now
                    param match {
                      case hp: HeaderParameter =>
                        new HeaderExtractor[AnyRef](hp.name, identity)
                      case UnhandledParameter(p) =>
                        throw new RuntimeException(
                          s"Unhandled parameter for [${serviceMethod.methodName}]: [$p], message type: " + inputMessageName)
                      // FIXME not handled: BodyParameter(_, _), PathParameter(_, _), QueryParamParameter(_, _)
                    }
                }
              }.toArray
            } else Array.empty

          ComponentMethod(
            grpcMethodName,
            parameterExtractors,
            message,
            Seq(TypeUrl2Method(message.getFullName, method.javaMethod)),
            kalixMethod)
           //FIXME make sure messe.getFullName is fine
        case method: CombinedSubscriptionServiceMethod =>
          val parameterExtractors: ParameterExtractors =
            method.typeUrl2Method
              .flatMap(each =>
                each.method.getParameterTypes.map(param => new ParameterExtractors.AnyBodyExtractor[AnyRef](param)))
              .toArray
          ComponentMethod(
            grpcMethodName,
            parameterExtractors,
            JavaPbAny.getDescriptor,
            method.typeUrl2Method,
            kalixMethod
          )
        case method: AnyServiceMethod =>
          val message = fileDescriptor.findMessageTypeByName(inputMessageName)
          // methods that receive Any as input always default to AnyBodyExtractor
          val parameterExtractors: ParameterExtractors = Array(
            new ParameterExtractors.AnyBodyExtractor(method.inputType))
          val typeUrl2method = serviceMethod.javaMethodOpt match {
            case Some(m) => Seq(TypeUrl2Method(m.getName, m)) //FIXME should not be m.getName Views fail because of this
            case None    => Nil
          }
          ComponentMethod(
            grpcMethodName,
            parameterExtractors,
            JavaPbAny.getDescriptor,
            typeUrl2method,
            kalixMethod
          )
      }

    }
  }
  //FIXME both are wrong: adding the name of the method to typeUrl2Method
  // and lookupMethod passing the messageName
  //FIXME? why when we generate synthetic rpc method the message is Any
  // and when not we generate a Synthetic Message name
  private def buildSyntheticMessageAndExtractors(
      nameGenerator: NameGenerator,
      serviceMethod: SpringRestServiceMethod,
      httpRule: HttpRule.Builder,
      entityKeys: Seq[String] = Seq.empty): (DescriptorProto, Map[Int, ExtractorCreator]) = {

    val inputMessageName = nameGenerator.getName(serviceMethod.methodName.capitalize + "KalixSyntheticRequest")

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

  private def buildGrpcMethod(
      grpcMethodName: String,
      inputTypeName: String,
      streamIn: Boolean,
      streamOut: Boolean): MethodDescriptorProto.Builder =
    MethodDescriptorProto
      .newBuilder()
      .setName(grpcMethodName)
      .setInputType(inputTypeName)
      .setClientStreaming(streamIn)
      .setServerStreaming(streamOut)
      .setOutputType("google.protobuf.Any")

}

private[springsdk] final case class ComponentDescriptor private (
    serviceName: String,
    packageName: String,
    methods: Map[String, ComponentMethod],
    serviceDescriptor: Descriptors.ServiceDescriptor,
    fileDescriptor: Descriptors.FileDescriptor)
