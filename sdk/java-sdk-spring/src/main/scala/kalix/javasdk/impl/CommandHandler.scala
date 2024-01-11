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

import java.lang.reflect.Method

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.google.protobuf.Descriptors
import kalix.javasdk.impl.reflection.ParameterExtractor
import org.slf4j.LoggerFactory

case class CommandHandler(
    grpcMethodName: String,
    messageCodec: JsonMessageCodec,
    requestMessageDescriptor: Descriptors.Descriptor,
    methodInvokers: Map[String, MethodInvoker]) {

  val logger = LoggerFactory.getLogger(classOf[CommandHandler])

  /**
   * This method will look up for a registered method that receives a super type of the incoming payload. It's only
   * called when a direct method is not found.
   *
   * The incoming typeUrl is for one of the existing sub types, but the method itself is defined to receive a super
   * type. Therefore we look up the method parameter to find out if one of its sub types matches the incoming typeUrl.
   */
  private def lookupMethodAcceptingSubType(inputTypeUrl: String): Option[MethodInvoker] = {
    methodInvokers.values.find { javaMethod =>
      val lastParam = javaMethod.method.getParameterTypes.last
      if (lastParam.getAnnotation(classOf[JsonSubTypes]) != null) {
        lastParam.getAnnotation(classOf[JsonSubTypes]).value().exists { subType =>
          inputTypeUrl == messageCodec
            .typeUrlFor(subType.value()) //TODO requires more changes to be used with JsonMigration
        }
      } else false
    }
  }

  def lookupInvoker(inputTypeUrl: String): Option[MethodInvoker] =
    methodInvokers
      .get(messageCodec.removeVersion(inputTypeUrl))
      .orElse(lookupMethodAcceptingSubType(inputTypeUrl))

  def getInvoker(inputTypeUrl: String): MethodInvoker =
    lookupInvoker(inputTypeUrl).getOrElse {
      throw new NoSuchElementException(
        s"Couldn't find any entry for typeUrl [$inputTypeUrl] in [${methodInvokers.view.mapValues(_.method.getName)}].")
    }
}

object MethodInvoker {

  def apply(javaMethod: Method, parameterExtractor: ParameterExtractor[InvocationContext, AnyRef]): MethodInvoker =
    MethodInvoker(javaMethod, Array(parameterExtractor))

}

case class MethodInvoker(method: Method, parameterExtractors: Array[ParameterExtractor[InvocationContext, AnyRef]]) {

  /**
   * To invoke methods with parameters an InvocationContext is necessary extract them from the message.
   */
  def invoke(componentInstance: AnyRef, invocationContext: InvocationContext): AnyRef =
    method.invoke(componentInstance, parameterExtractors.map(e => e.extract(invocationContext)): _*)

  /**
   * To invoke methods with arity zero.
   */
  def invoke(componentInstance: AnyRef): AnyRef =
    method.invoke(componentInstance)
}
