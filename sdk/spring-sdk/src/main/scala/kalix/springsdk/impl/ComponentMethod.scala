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

// Might need to have one of each of these for unary, streamed out, streamed in and streamed.
/**
 * @param method
 *   method from the annotated class
 * @param grpcMethodName
 *   'rpc' method name. When it has a methodsMap - see below - the name is not the same as the method above but a
 *   synthetic one.
 * @param parameterExtractors
 *   To extract the value of a parameter from protobuf.Any
 * @param requestMessageDescriptor
 * @param inputClass2Method
 *   This component method may represent multiple methods in a class. For example, if a class is annotated with the same
 * @Subscription.Topic("xyz")
 *   in two different methods then this methodsMap keeps reference to those two methods.
 */
case class ComponentMethod(
    method: Option[Method],
    grpcMethodName: String,
    parameterExtractors: Array[ParameterExtractor[InvocationContext, AnyRef]],
    requestMessageDescriptor: Descriptors.Descriptor,
    inputClass2Method: Map[String, Method] = Map()) {

  def lookupMethod(inputTypeUrl: String): JavaMethod = {
    if (inputClass2Method.isEmpty) {
      JavaMethod(method, parameterExtractors)
    } else {
      val method = inputClass2Method.get(inputTypeUrl)
      val methodParameterTypes = method.get.getParameterTypes();
      val extractors = parameterExtractors.collect {
        //FIXME is it safe to pick the last parameter. An action has one and View has two. Is it in the View always the second the event? are we enforcing that?
        case extractor @ AnyBodyExtractor(cls)
            if cls.getName.equals(methodParameterTypes(methodParameterTypes.size - 1).getName) =>
          extractor.asInstanceOf[ParameterExtractor[InvocationContext, AnyRef]]
      }
      JavaMethod(method, extractors)
    }
  }
}

case class JavaMethod(method: Option[Method], parameterExtractors: Array[ParameterExtractor[InvocationContext, AnyRef]])
