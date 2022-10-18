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

import com.google.protobuf.DescriptorProtos
import com.google.protobuf.Descriptors
import kalix.PrincipalMatcher
import kalix.{ Acl => ProtoAcl }
import kalix.springsdk.annotations.Acl
import kalix.springsdk.impl.ProtoDescriptorGenerator.dependencies
import org.slf4j.LoggerFactory
import kalix.{ Annotations => KalixAnnotations }

object AclDescriptorFactory {

  private val logger = LoggerFactory.getLogger(classOf[AclDescriptorFactory.type])

  val invalidAnnotationUsage =
    "Invalid annotation usage. Matcher has both 'principal' and 'service' defined. " +
    "Only one is allowed."

  private def validateMatcher(matcher: Acl.Matcher) = {
    if (matcher.principal() != Acl.Principal.UNSPECIFIED && matcher.service().nonEmpty)
      throw new IllegalArgumentException(invalidAnnotationUsage)
  }

  private def deriveProtoAnnotation(aclJavaAnnotation: Acl): ProtoAcl = {

    aclJavaAnnotation.allow().foreach(matcher => validateMatcher(matcher))
    aclJavaAnnotation.deny().foreach(matcher => validateMatcher(matcher))

    val aclBuilder = ProtoAcl.newBuilder()

    aclJavaAnnotation.allow.zipWithIndex.foreach { case (allow, idx) =>
      val principalMatcher = PrincipalMatcher.newBuilder()
      if (allow.principal != Acl.Principal.UNSPECIFIED) {
        allow.principal match {
          case Acl.Principal.ALL =>
            principalMatcher.setPrincipal(PrincipalMatcher.Principal.ALL)
          case Acl.Principal.INTERNET =>
            principalMatcher.setPrincipal(PrincipalMatcher.Principal.INTERNET)
        }
        aclBuilder.addAllow(idx, principalMatcher)

      } else {
        principalMatcher.setService(allow.service())
        aclBuilder.addAllow(idx, principalMatcher)
      }
    }

    aclJavaAnnotation.deny.zipWithIndex.foreach { case (deny, idx) =>
      val principalMatcher = PrincipalMatcher.newBuilder()
      if (deny.principal != Acl.Principal.UNSPECIFIED) {
        deny.principal match {
          case Acl.Principal.ALL =>
            principalMatcher.setPrincipal(PrincipalMatcher.Principal.ALL)
          case Acl.Principal.INTERNET =>
            principalMatcher.setPrincipal(PrincipalMatcher.Principal.INTERNET)
        }
        aclBuilder.addDeny(idx, principalMatcher)

      } else {
        principalMatcher.setService(deny.service())
        aclBuilder.addDeny(idx, principalMatcher)
      }
    }

    aclBuilder.setDenyCode(aclJavaAnnotation.denyCode())

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
          .setPackage("kalix.springsdk")

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
}
