/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl

import java.lang.reflect.Method

import com.google.protobuf.DescriptorProtos
import com.google.protobuf.Descriptors
import kalix.PrincipalMatcher
import kalix.javasdk.annotations.Acl
import kalix.{ Acl => ProtoAcl }
import kalix.{ Annotations => KalixAnnotations }
import org.slf4j.LoggerFactory

object AclDescriptorFactory {

  private val logger = LoggerFactory.getLogger(classOf[AclDescriptorFactory.type])

  val invalidAnnotationUsage: String =
    "Invalid annotation usage. Matcher has both 'principal' and 'service' defined. " +
    "Only one is allowed."

  private def validateMatcher(matcher: Acl.Matcher): Unit = {
    if (matcher.principal() != Acl.Principal.UNSPECIFIED && matcher.service().nonEmpty)
      throw new IllegalArgumentException(invalidAnnotationUsage)
  }

  private def deriveProtoAnnotation(aclJavaAnnotation: Acl): ProtoAcl = {

    aclJavaAnnotation.allow().foreach(matcher => validateMatcher(matcher))
    aclJavaAnnotation.deny().foreach(matcher => validateMatcher(matcher))

    val aclBuilder = ProtoAcl.newBuilder()

    aclJavaAnnotation.allow.zipWithIndex.foreach { case (allow, idx) =>
      val principalMatcher = PrincipalMatcher.newBuilder()
      allow.principal match {
        case Acl.Principal.ALL =>
          principalMatcher.setPrincipal(PrincipalMatcher.Principal.ALL)
        case Acl.Principal.INTERNET =>
          principalMatcher.setPrincipal(PrincipalMatcher.Principal.INTERNET)
        case Acl.Principal.UNSPECIFIED =>
          principalMatcher.setService(allow.service())
      }

      aclBuilder.addAllow(idx, principalMatcher)
    }

    aclJavaAnnotation.deny.zipWithIndex.foreach { case (deny, idx) =>
      val principalMatcher = PrincipalMatcher.newBuilder()
      deny.principal match {
        case Acl.Principal.ALL =>
          principalMatcher.setPrincipal(PrincipalMatcher.Principal.ALL)
        case Acl.Principal.INTERNET =>
          principalMatcher.setPrincipal(PrincipalMatcher.Principal.INTERNET)
        case Acl.Principal.UNSPECIFIED =>
          principalMatcher.setService(deny.service())

      }
      aclBuilder.addDeny(idx, principalMatcher)
    }

    if (aclJavaAnnotation.inheritDenyCode()) {
      aclBuilder.setDenyCode(0)
    } else {
      aclBuilder.setDenyCode(aclJavaAnnotation.denyCode().value)
    }

    aclBuilder.build()
  }

  def defaultAclFileDescriptor(cls: Class[_]): Option[DescriptorProtos.FileDescriptorProto] = {

    Option.when(cls.getAnnotation(classOf[Acl]) != null) {
      // do we need to recurse into the dependencies of the dependencies? Probably not, just top level imports.
      val dependencies: Array[Descriptors.FileDescriptor] = Array(KalixAnnotations.getDescriptor)

      val policyFile = "kalix_policy.proto"

      val protoBuilder =
        DescriptorProtos.FileDescriptorProto.newBuilder
          .setName(policyFile)
          .setSyntax("proto3")
          .setPackage("kalix.javasdk")

      val kalixFileOptions = kalix.FileOptions.newBuilder
      kalixFileOptions.setAcl(deriveProtoAnnotation(cls.getAnnotation(classOf[Acl])))

      val options =
        DescriptorProtos.FileOptions
          .newBuilder()
          .setExtension(kalix.Annotations.file, kalixFileOptions.build())
          .build()

      protoBuilder.setOptions(options)
      val fdProto = protoBuilder.build
      val fd = Descriptors.FileDescriptor.buildFrom(fdProto, dependencies)
      if (logger.isDebugEnabled) {
        logger.debug(
          "Generated file descriptor for service [{}]: \n{}",
          policyFile,
          ProtoDescriptorRenderer.toString(fd))
      }
      fd.toProto
    }
  }

  def serviceLevelAclAnnotation(component: Class[_]): Option[kalix.ServiceOptions] = {

    val javaAclAnnotation = component.getAnnotation(classOf[Acl])

    Option.when(javaAclAnnotation != null) {
      val kalixServiceOptions = kalix.ServiceOptions.newBuilder()
      kalixServiceOptions.setAcl(deriveProtoAnnotation(javaAclAnnotation))
      kalixServiceOptions.build()
    }
  }

  def methodLevelAclAnnotation(method: Method): Option[kalix.MethodOptions] = {

    val javaAclAnnotation = method.getAnnotation(classOf[Acl])

    Option.when(javaAclAnnotation != null) {
      val kalixServiceOptions = kalix.MethodOptions.newBuilder()
      kalixServiceOptions.setAcl(deriveProtoAnnotation(javaAclAnnotation))
      kalixServiceOptions.build()
    }
  }

}
