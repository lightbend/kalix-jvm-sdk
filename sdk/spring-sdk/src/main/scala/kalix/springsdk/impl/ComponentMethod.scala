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

import com.fasterxml.jackson.annotation.JsonSubTypes

import java.lang.reflect.Method
import com.google.protobuf.Descriptors
import kalix.javasdk.JsonSupport
import kalix.javasdk.action.Action
import kalix.javasdk.view.View
import kalix.springsdk.impl.reflection.KalixMethod
import kalix.springsdk.impl.reflection.ParameterExtractor
import kalix.springsdk.impl.reflection.ParameterExtractors.AnyBodyExtractor
import kalix.springsdk.impl.reflection.ParameterExtractors.FieldExtractor
import org.slf4j.LoggerFactory

// Might need to have one of each of these for unary, streamed out, streamed in and streamed.
/**
 * @param grpcMethodName
 *   'rpc' method name. When it has a methodsMap - see below - the name is not the same as the method above but a
 *   synthetic one.
 * @param parameterExtractors
 *   To extract the value of a parameter from protobuf.Any
 * @param requestMessageDescriptor
 * @param typeUrl2Methods
 *   This is the list of reflect.Method available for each typeUrl
 */
case class ComponentMethod(
    grpcMethodName: String,
    parameterExtractors: Array[ParameterExtractor[InvocationContext, AnyRef]],
    requestMessageDescriptor: Descriptors.Descriptor,
    typeUrl2Methods: Seq[TypeUrl2Method] = Nil,
    componentClass: Class[_],
    kalixMethod: KalixMethod) { //TODO maybe passing only ignore value so not to leak abstractions?

  val logger = LoggerFactory.getLogger(ComponentMethod.getClass)

  def lookupMethod(inputTypeUrl: String, componentClazz: Class[_]): Option[JavaMethod] = {
    val method = typeUrl2Methods.find(p =>
      "type.googleapis.com/" + p.typeUrl == inputTypeUrl ||
      p.typeUrl == inputTypeUrl ||
      hasJsonSubType(
        p.method.getParameterTypes()(p.method.getParameterTypes.length - 1),
        inputTypeUrl.replace(JsonSupport.KALIX_JSON, "")) ||
      p.method
        .getParameterTypes()(p.method.getParameterTypes.length - 1)
        .getName
        .equals(inputTypeUrl.replace(JsonSupport.KALIX_JSON, "")))
    //FIXME havoc! would it work to call back to component to rebuild the typeUrl directly from the component?
    // In doing so if data is saved with one SDK and later logic changes that data becomes unrecoverable?
    method match {
      case Some(meth) =>
        Some(JavaMethod(meth.method, getExtractors(meth.method, componentClazz)))
      case None if isIgnore(kalixMethod) =>
        None
      case None =>
        throw NoRouteFoundException(s"Couldn't find any entry for typeUrl [${inputTypeUrl}] in [${typeUrl2Methods}].")

    }
  }

  private def hasJsonSubType(clazz: Class[_], withValue: String): Boolean = {
    val ann = clazz.getAnnotation(classOf[JsonSubTypes])
    if (ann != null) {
      ann.value().toList match {
        case seq =>
          seq.exists { m =>
            m.name() == withValue || m.value() == withValue
          }
        case Nil => false
      }
    } else false
  }

  def isIgnore(kalixMethod: KalixMethod): Boolean = {
    kalixMethod.methodOptions.exists(m => m.hasEventing && m.getEventing.hasIn && m.getEventing.getIn.getIgnore)
  }

  private def getExtractors(method: Method, clazz: Class[_]): Array[ParameterExtractor[InvocationContext, AnyRef]] = {
    val methodParameterTypes = method.getParameterTypes();
    val p = parameterExtractors.collect {
      //it is safe to pick the last parameter. An action has one and View has two. In the View the event it is always the last parameter
      case extractor @ AnyBodyExtractor(cls)
          if cls.getName.equals(methodParameterTypes(methodParameterTypes.size - 1).getName) =>
        extractor.asInstanceOf[ParameterExtractor[InvocationContext, AnyRef]]
      //TODO make sure this extractor is as
      case extractor @ FieldExtractor(field, _)
          if clazz.equals(classOf[View[_]]) &&
            methodParameterTypes(methodParameterTypes.size - 1).getName
              .toUpperCase()
              .contains(field.getType.getJavaType.name()) =>
        extractor.asInstanceOf[ParameterExtractor[InvocationContext, AnyRef]]
      case extractor @ FieldExtractor(field, _)
          if clazz.equals(classOf[Action]) &&
            methodParameterTypes.toList.exists { mpt =>
              mpt.getName
                .toUpperCase()
                .contains(field.getType.getJavaType.name())
            } =>
        extractor.asInstanceOf[ParameterExtractor[InvocationContext, AnyRef]]
    }
    p
  }
}

case class TypeUrl2Method(typeUrl: String, method: Method)

case class JavaMethod(method: Method, parameterExtractors: Array[ParameterExtractor[InvocationContext, AnyRef]])

case class NoRouteFoundException(message: String) extends RuntimeException
