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

import java.lang.reflect.Type

import scala.annotation.tailrec

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.api.AnnotationsProto
import com.google.api.CustomHttpPattern
import com.google.api.HttpRule
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.DescriptorProtos.DescriptorProto
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto
import com.google.protobuf.DescriptorProtos.MethodOptions
import com.google.protobuf.ByteString
import com.google.protobuf.Descriptors
import kalix.springsdk.impl.reflection.RestServiceIntrospector.BodyParameter
import kalix.springsdk.impl.reflection.RestServiceIntrospector.PathParameter
import kalix.springsdk.impl.reflection.RestServiceIntrospector.QueryParamParameter
import kalix.springsdk.impl.reflection.RestServiceIntrospector.RestMethod
import org.springframework.web.bind.annotation.RequestMethod

case class DynamicMethodInfo(
    restMethod: RestMethod,
    descriptor: DescriptorProto,
    method: MethodDescriptorProto,
    extractors: Seq[(Int, ExtractorCreator)])

object DynamicMethodInfo {

  def build(
      restMethod: RestMethod,
      generator: NameGenerator,
      mapper: ObjectMapper,
      entityKeys: Seq[String] = Seq.empty): DynamicMethodInfo = {

    val methodName = restMethod.method.getName.capitalize
    val inputTypeName = generator.getName(methodName + "Request")

    val method = MethodDescriptorProto.newBuilder()
    method.setName(generator.getName(methodName))
    method.setInputType(inputTypeName)
    method.setOutputType("google.protobuf.Any")

    val httpRule = HttpRule.newBuilder()
    val pathTemplate = restMethod.parsedPath.toGrpcTranscodingPattern
    restMethod.requestMethod match {
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

    val descriptor = DescriptorProto.newBuilder()
    descriptor.setName(inputTypeName)

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

    val indexedParams = restMethod.params.zipWithIndex
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
      descriptor.addField(fieldDescriptor)
      val fieldReader = mapper.readerFor(mapper.constructType(param.getGenericParameterType))
      idx -> new ExtractorCreator {
        override def apply(descriptor: Descriptors.Descriptor): ParameterExtractor[DynamicMessageContext, AnyRef] =
          new ParameterExtractors.BodyExtractor(descriptor.findFieldByNumber(1), fieldReader)
      }
    }

    val pathParamFields = restMethod.parsedPath.fields.zipWithIndex.flatMap { case (paramName, fieldIdx) =>
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
      descriptor.addField(fieldDescriptor)
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
        descriptor.addField(fieldDescriptor)
        // todo entity key
        paramIdx -> new ExtractorCreator {
          override def apply(descriptor: Descriptors.Descriptor): ParameterExtractor[DynamicMessageContext, AnyRef] = {
            new ParameterExtractors.FieldExtractor[AnyRef](descriptor.findFieldByNumber(fieldNumber), identity)
          }
        }
      }

    val methodOptions = MethodOptions
      .newBuilder()
      .setExtension(AnnotationsProto.http, httpRule.build())
    method.setOptions(methodOptions)

    DynamicMethodInfo(restMethod, descriptor.build(), method.build(), bodyField.toSeq ++ pathParamFields ++ queryFields)
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
}

trait ExtractorCreator {
  def apply(descriptor: Descriptors.Descriptor): ParameterExtractor[DynamicMessageContext, AnyRef]
}

/**
 * Ensures all generated names in a given package are unique, noting that method names and message names must not
 * conflict
 */
class NameGenerator {
  private var names: Set[String] = Set.empty

  def getName(base: String): String = {
    if (names(base)) {
      incrementName(base, 1)
    } else {
      names += base
      base
    }
  }

  @tailrec
  private def incrementName(base: String, inc: Int): String = {
    val name = base + inc
    if (names(name)) {
      incrementName(base, inc + 1)
    } else {
      names += name
      name
    }
  }
}
