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

import kalix.KeyGeneratorMethodOptions.Generator
import kalix.javasdk.impl.JwtDescriptorFactory.buildJWTOptions
import kalix.javasdk.impl.ComponentDescriptorFactory.mergeServiceOptions
import kalix.javasdk.impl.reflection.IdExtractor.extractIds
import kalix.javasdk.impl.reflection.KalixMethod
import kalix.javasdk.impl.reflection.NameGenerator
import kalix.javasdk.impl.reflection.RestServiceIntrospector

private[impl] object EntityDescriptorFactory extends ComponentDescriptorFactory {

  override def buildDescriptorFor(
      component: Class[_],
      messageCodec: JsonMessageCodec,
      nameGenerator: NameGenerator): ComponentDescriptor = {

    val kalixMethods =
      RestServiceIntrospector.inspectService(component).methods.map { restMethod =>

        val ids = extractIds(component, restMethod.javaMethod)

        val kalixMethod =
          if (ids.isEmpty) {
            val idGenOptions = kalix.IdGeneratorMethodOptions.newBuilder().setAlgorithm(Generator.VERSION_4_UUID)
            val methodOpts = kalix.MethodOptions.newBuilder().setIdGenerator(idGenOptions)
            KalixMethod(restMethod).withKalixOptions(methodOpts.build())
          } else {
            KalixMethod(restMethod, entityIds = ids)
          }

        kalixMethod.withKalixOptions(buildJWTOptions(restMethod.javaMethod))
      }

    val serviceName = nameGenerator.getName(component.getSimpleName)
    ComponentDescriptor(
      nameGenerator,
      messageCodec,
      serviceName,
      serviceOptions = mergeServiceOptions(
        AclDescriptorFactory.serviceLevelAclAnnotation(component),
        JwtDescriptorFactory.serviceLevelJwtAnnotation(component)),
      component.getPackageName,
      kalixMethods)
  }
}
