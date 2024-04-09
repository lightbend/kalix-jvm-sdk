/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl

import java.lang.reflect.Method
import kalix.JwtMethodOptions
import kalix.JwtServiceOptions
import kalix.MethodOptions
import kalix.javasdk.annotations.JWT
import kalix.javasdk.impl.reflection.Reflect.Syntax._
import kalix.JwtStaticClaim

import scala.jdk.CollectionConverters.IterableHasAsJava

object JwtDescriptorFactory {

  private def buildStaticClaimFromAnnotation(sc: JWT.StaticClaim): JwtStaticClaim =
    JwtStaticClaim
      .newBuilder()
      .setClaim(sc.claim())
      .addAllValue(sc.value().toList.asJava)
      .setPattern(sc.pattern())
      .build()

  private def jwtMethodOptions(javaMethod: Method): JwtMethodOptions = {
    val ann = javaMethod.getAnnotation(classOf[JWT])
    val jwt = JwtMethodOptions.newBuilder()
    ann
      .validate()
      .map(springValidate => jwt.addValidate(JwtMethodOptions.JwtMethodMode.forNumber(springValidate.ordinal())))
    ann.bearerTokenIssuer().map(jwt.addBearerTokenIssuer)

    ann
      .staticClaims()
      .foreach(sc => jwt.addStaticClaim(buildStaticClaimFromAnnotation(sc)))
    jwt.build()
  }

  private def hasServiceLevelJwt(clazz: Class[_]): Boolean =
    clazz.isPublic && clazz.hasAnnotation[JWT]

  private def hasJwtMethodOptions(javaMethod: Method): Boolean = {
    javaMethod.isPublic && javaMethod.hasAnnotation[JWT]
  }

  def buildJWTOptions(method: Method): Option[MethodOptions] =
    Option.when(hasJwtMethodOptions(method)) {
      kalix.MethodOptions.newBuilder().setJwt(jwtMethodOptions(method)).build()
    }
  def jwtOptions(method: Method): Option[JwtMethodOptions] =
    Option.when(hasJwtMethodOptions(method)) {
      jwtMethodOptions(method)
    }

  def serviceLevelJwtAnnotation(component: Class[_]): Option[kalix.ServiceOptions] =
    Option.when(hasServiceLevelJwt(component)) {
      val ann = component.getAnnotation(classOf[JWT])
      val jwt = JwtServiceOptions.newBuilder()
      ann
        .validate()
        .map(methodMode => jwt.setValidate(JwtServiceOptions.JwtServiceMode.forNumber(methodMode.ordinal())))
      ann.bearerTokenIssuer().map(jwt.addBearerTokenIssuer)
      ann
        .staticClaims()
        .foreach(sc => jwt.addStaticClaim(buildStaticClaimFromAnnotation(sc)))

      val kalixServiceOptions = kalix.ServiceOptions.newBuilder()
      kalixServiceOptions.setJwt(jwt.build())
      kalixServiceOptions.build()
    }
}
