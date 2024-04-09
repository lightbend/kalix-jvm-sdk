/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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
