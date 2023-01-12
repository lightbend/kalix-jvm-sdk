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

import kalix.KeyGeneratorMethodOptions.Generator
import kalix.springsdk.annotations.EntityKey
import kalix.springsdk.annotations.GenerateEntityKey
import kalix.springsdk.annotations.WorkflowKey
import kalix.springsdk.impl.ComponentDescriptorFactory.buildJWTOptions
import kalix.springsdk.impl.reflection.KalixMethod
import kalix.springsdk.impl.reflection.NameGenerator
import kalix.springsdk.impl.reflection.RestServiceIntrospector
import kalix.springsdk.impl.reflection.ServiceIntrospectionException

private[impl] object WorkflowDescriptorFactory extends ComponentDescriptorFactory {

  override def buildDescriptorFor(
      component: Class[_],
      messageCodec: SpringSdkMessageCodec,
      nameGenerator: NameGenerator): ComponentDescriptor = {

    val workflowKeysOnType = {
      val anno = component.getAnnotation(classOf[WorkflowKey])
      if (anno != null) anno.value()
      else Array.empty[String]
    }

    val kalixMethods =
      RestServiceIntrospector.inspectService(component).methods.map { restMethod =>

        val workflowKeyOnMethod = restMethod.javaMethod.getAnnotation(classOf[WorkflowKey])
        val generateEntityKey = restMethod.javaMethod.getAnnotation(classOf[GenerateEntityKey])

        if (workflowKeyOnMethod != null && generateEntityKey != null)
          throw ServiceIntrospectionException(
            restMethod.javaMethod,
            "Invalid annotation usage. Found both @WorkflowKey and @GenerateEntityKey annotations. " +
            "A method can only be annotated with one of them, but not both.")

        val kalixMethod =
          if (generateEntityKey != null) {
            val keyGenOptions = kalix.KeyGeneratorMethodOptions.newBuilder().setKeyGenerator(Generator.VERSION_4_UUID)
            val methodOpts = kalix.MethodOptions.newBuilder().setEntity(keyGenOptions)
            KalixMethod(restMethod).withKalixOptions(methodOpts.build())

          } else {
            // keys defined on Method level get precedence
            val workflowKeysToUse =
              if (workflowKeyOnMethod != null) workflowKeyOnMethod.value()
              else workflowKeysOnType

            if (workflowKeysToUse.isEmpty)
              throw ServiceIntrospectionException(
                restMethod.javaMethod,
                "Invalid command method. No @WorkflowKey nor @GenerateEntityKey annotations found. " +
                "A command method should be annotated with either @WorkflowKey or @GenerateEntityKey, or " +
                "an @WorkflowKey annotation should be present at class level.")

            KalixMethod(restMethod, entityKeys = workflowKeysToUse.toIndexedSeq)
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
