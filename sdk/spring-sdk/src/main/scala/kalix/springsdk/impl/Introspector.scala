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

import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto
import kalix.springsdk.annotations.EntityKey
import kalix.springsdk.impl.reflection.DynamicMethodInfo
import kalix.springsdk.impl.reflection.NameGenerator
import kalix.springsdk.impl.reflection.ParameterExtractors.HeaderExtractor
import kalix.springsdk.impl.reflection.RestServiceIntrospector
import kalix.springsdk.impl.reflection.RestServiceIntrospector.HeaderParameter
import kalix.springsdk.impl.reflection.RestServiceIntrospector.UnhandledParameter
import com.google.protobuf.{ Any => JavaPbAny }
import com.google.protobuf.any.{ Any => ScalaPbAny }

object Introspector {

  def inspect[T](component: Class[T]): ComponentDescription = {

    val nameGenerator = new NameGenerator
    val restService = RestServiceIntrospector.inspectService(component)

    val grpcService = ServiceDescriptorProto.newBuilder()
    // FIXME: I made NameGenerator internal state, but we need unique names per service as well
    // on the other hand, using the same NameGenerator for all components will make it hard to debug
    // as we it will make it hard to correlate the methods and to the classes they are defined.
    grpcService.setName(nameGenerator.getName(component.getSimpleName))

    val declaredEntityKeys: Seq[String] =
      Option(component.getAnnotation(classOf[EntityKey]))
        .map(_.value())
        .toSeq
        .flatten

    val methodsInfo =
      restService.methods.map(restMethod => DynamicMethodInfo.build(restMethod, nameGenerator, declaredEntityKeys))

    val messageDescriptors = methodsInfo.flatMap { methodInfo =>
      grpcService.addMethod(methodInfo.grpcMethod)
      methodInfo.inputMessageDescriptor
    }

    val fileDescriptor = ProtoDescriptorGenerator.genFileDescriptor(
      component.getName,
      component.getPackageName,
      grpcService.build(),
      messageDescriptors)

    val serviceDescriptor = fileDescriptor.findServiceByName(grpcService.getName)

    val methods = methodsInfo.map { method =>

      val message = method.inputMessageDescriptor
        .map { inputDescriptor =>
          fileDescriptor.findMessageTypeByName(inputDescriptor.getName)
        }
        .getOrElse(ScalaPbAny.javaDescriptor)

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
      method.grpcMethod.getName -> ComponentMethod(
        method.restMethod.javaMethod,
        method.grpcMethod.getName,
        extractors.toArray,
        message)
    }.toMap

    new ComponentDescription(fileDescriptor, serviceDescriptor, methods)
  }

}
