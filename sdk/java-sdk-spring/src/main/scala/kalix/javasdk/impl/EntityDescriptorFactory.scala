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

package kalix.javasdk.impl

import java.lang.reflect.AnnotatedElement

import kalix.KeyGeneratorMethodOptions.Generator
import kalix.javasdk.annotations.EntityKey
import kalix.javasdk.annotations.GenerateEntityKey
import kalix.javasdk.annotations.GenerateId
import kalix.javasdk.annotations.Id
import kalix.javasdk.impl.ComponentDescriptorFactory.buildJWTOptions
import kalix.javasdk.impl.reflection.KalixMethod
import kalix.javasdk.impl.reflection.NameGenerator
import kalix.javasdk.impl.reflection.RestServiceIntrospector
import kalix.javasdk.impl.reflection.ServiceIntrospectionException

private[impl] object EntityDescriptorFactory extends ComponentDescriptorFactory {

  override def buildDescriptorFor(
      component: Class[_],
      messageCodec: JsonMessageCodec,
      nameGenerator: NameGenerator): ComponentDescriptor = {

    def idValue(annotatedElement: AnnotatedElement) =
      if (annotatedElement.getAnnotation(classOf[Id]) != null)
        annotatedElement.getAnnotation(classOf[Id]).value()
      else if (annotatedElement.getAnnotation(classOf[EntityKey]) != null)
        annotatedElement.getAnnotation(classOf[EntityKey]).value()
      else
        Array.empty[String]

    def shouldGenerateId(annotatedElement: AnnotatedElement) =
      if (annotatedElement.getAnnotation(classOf[GenerateId]) != null)
        true
      else
        annotatedElement.getAnnotation(classOf[GenerateEntityKey]) != null

    val idOnType = idValue(component)

    val kalixMethods =
      RestServiceIntrospector.inspectService(component).methods.map { restMethod =>

        val entityIdOnMethod = idValue(restMethod.javaMethod)
        val generateEntityId = shouldGenerateId(restMethod.javaMethod)
        if (entityIdOnMethod.nonEmpty && generateEntityId)
          throw ServiceIntrospectionException(
            restMethod.javaMethod,
            "Invalid annotation usage. Found both @Id and @GenerateId annotations. " +
            "A method can only be annotated with one of them, but not both.")

        val kalixMethod =
          if (generateEntityId) {
            val keyGenOptions = kalix.KeyGeneratorMethodOptions.newBuilder().setKeyGenerator(Generator.VERSION_4_UUID)
            val methodOpts = kalix.MethodOptions.newBuilder().setEntity(keyGenOptions)
            KalixMethod(restMethod).withKalixOptions(methodOpts.build())

          } else {
            // keys defined on Method level get precedence
            val entityKeysToUse =
              if (entityIdOnMethod.nonEmpty) entityIdOnMethod
              else idOnType

            if (entityKeysToUse.isEmpty)
              throw ServiceIntrospectionException(
                restMethod.javaMethod,
                "Invalid command method. No @Id nor @GenerateId annotations found. " +
                "A command method should be annotated with either @Id or @GenerateId, or " +
                "an @Id annotation should be present at class level.")

            KalixMethod(restMethod, entityKeys = entityKeysToUse.toIndexedSeq)
          }

        kalixMethod.withKalixOptions(buildJWTOptions(restMethod.javaMethod))
      }

    val serviceName = nameGenerator.getName(component.getSimpleName)
    ComponentDescriptor(
      nameGenerator,
      messageCodec,
      serviceName,
      serviceOptions = AclDescriptorFactory.serviceLevelAclAnnotation(component),
      component.getPackageName,
      kalixMethods)
  }
}
