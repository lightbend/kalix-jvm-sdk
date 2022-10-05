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
import com.google.protobuf.Descriptors
import kalix.springsdk.impl.reflection.ParameterExtractor
import kalix.springsdk.impl.reflection.ParameterExtractors.AnyBodyExtractor
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
    typeUrl2Methods: Seq[TypeUrl2Method] = Nil) {

  val logger = LoggerFactory.getLogger(ComponentMethod.getClass)

  def lookupMethod(inputTypeUrl: String): JavaMethod = {
    val method = typeUrl2Methods.find(p => p.typeUrl == inputTypeUrl)
    method match {
      case Some(meth) =>
        JavaMethod(meth.method, getExtractors(meth.method))
      case None if typeUrl2Methods.size == 1 =>
        logger.warn(
          s"Couldn't find any entry for typeUrl [${inputTypeUrl}] in [${typeUrl2Methods}]." +
          s" Choosing the first option [${typeUrl2Methods.head.method}].")
        JavaMethod(typeUrl2Methods.head.method, parameterExtractors)
      case None =>
        throw new IllegalStateException(
          s"Couldn't find any entry for typeUrl [${inputTypeUrl}] in [${typeUrl2Methods}].")
    }
  }

  private def getExtractors(method: Method): Array[ParameterExtractor[InvocationContext, AnyRef]] = {
    val methodParameterTypes = method.getParameterTypes();
    parameterExtractors.collect {
      //it is safe to pick the last parameter. An action has one and View has two. In the View the event it is always the last parameter
      case extractor @ AnyBodyExtractor(cls)
          if cls.getName.equals(methodParameterTypes(methodParameterTypes.size - 1).getName) =>
        extractor.asInstanceOf[ParameterExtractor[InvocationContext, AnyRef]]
    }
  }
}

case class TypeUrl2Method(typeUrl: String, method: Method)

case class JavaMethod(method: Method, parameterExtractors: Array[ParameterExtractor[InvocationContext, AnyRef]])
