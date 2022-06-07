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

package kalix.springsdk.impl.action

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto
import com.google.protobuf.Descriptors
import kalix.javasdk.action.{ Action, ActionCreationContext }
import kalix.javasdk.impl.ActionFactory
import kalix.javasdk.impl.action.ActionRouter
import kalix.springsdk.impl.ProtoDescriptorGenerator
import kalix.springsdk.impl.reflection.ParameterExtractors.HeaderExtractor
import kalix.springsdk.impl.reflection.RestServiceIntrospector.{ HeaderParameter, UnhandledParameter }
import kalix.springsdk.impl.reflection.{ DynamicMethodInfo, NameGenerator, RestServiceIntrospector }

object ActionIntrospector {
  def inspect[A <: Action](
      action: Class[A],
      nameGenerator: NameGenerator,
      objectMapper: ObjectMapper,
      creator: () => A): SpringActionFactory[A] = {
    val restService = RestServiceIntrospector.inspectService(action)
    val grpcService = ServiceDescriptorProto.newBuilder()
    grpcService.setName(nameGenerator.getName(action.getSimpleName))
    val dynamicRestMethods =
      restService.methods.map(method => DynamicMethodInfo.build(method, nameGenerator, objectMapper))

    val messageDescriptors = dynamicRestMethods.map { method =>
      grpcService.addMethod(method.method)
      method.descriptor
    }

    val fileDescriptor = ProtoDescriptorGenerator.genFileDescriptor(
      action.getName,
      action.getPackageName,
      grpcService.build(),
      messageDescriptors)
    val serviceDescriptor = fileDescriptor.findServiceByName(grpcService.getName)
    val methods = dynamicRestMethods.map { method =>
      val message = fileDescriptor.findMessageTypeByName(method.descriptor.getName)
      val extractors = method.restMethod.params.zipWithIndex.map { case (param, idx) =>
        // First, see if we have an extractor for it to extract from the dynamic message
        method.extractors.find(_._1 == idx) match {
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
      }
      method.method.getName -> ActionMethod(
        method.restMethod.method,
        method.method.getName,
        extractors.toArray,
        message)
    }.toMap

    new SpringActionFactory(fileDescriptor, serviceDescriptor, creator, methods)
  }

}

class SpringActionFactory[A <: Action](
    val fileDescriptor: Descriptors.FileDescriptor,
    val serviceDescriptor: Descriptors.ServiceDescriptor,
    creator: () => A,
    methods: Map[String, ActionMethod])
    extends ActionFactory {
  override def create(context: ActionCreationContext): ActionRouter[A] =
    new ActionReflectiveRouter(creator(), methods)
}
