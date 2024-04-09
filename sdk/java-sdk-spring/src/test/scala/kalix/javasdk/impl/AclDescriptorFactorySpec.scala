/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl

import scala.reflect.ClassTag

import kalix.FileOptions
import kalix.PrincipalMatcher
import kalix.javasdk.annotations.Acl
import kalix.spring.testmodels.AclTestModels.MainAllowAllServices
import kalix.spring.testmodels.AclTestModels.MainAllowListOfServices
import kalix.spring.testmodels.AclTestModels.MainAllowPrincipalAll
import kalix.spring.testmodels.AclTestModels.MainAllowPrincipalInternet
import kalix.spring.testmodels.AclTestModels.MainDenyAllServices
import kalix.spring.testmodels.AclTestModels.MainDenyListOfServices
import kalix.spring.testmodels.AclTestModels.MainDenyPrincipalAll
import kalix.spring.testmodels.AclTestModels.MainDenyPrincipalInternet
import kalix.spring.testmodels.AclTestModels.MainDenyWithCode
import kalix.spring.testmodels.AclTestModels.MainWithInvalidAllowAnnotation
import kalix.spring.testmodels.AclTestModels.MainWithInvalidDenyAnnotation
import kalix.spring.testmodels.AclTestModels.MainWithoutAnnotation
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AclDescriptorFactorySpec extends AnyWordSpec with Matchers {

  def lookupExtension[T: ClassTag]: Option[FileOptions] =
    AclDescriptorFactory
      .defaultAclFileDescriptor(implicitly[ClassTag[T]].runtimeClass)
      .map { aclFileDesc =>
        aclFileDesc.getOptions.getExtension(kalix.Annotations.file)
      }

  "AclDescriptorFactory.defaultAclFileDescriptor" should {

    "generate an empty descriptor if no ACL annotation is found" in {
      val extension = lookupExtension[MainWithoutAnnotation]
      extension shouldBe empty
    }

    "generate a default ACL file descriptor with deny code" in {
      val extension = lookupExtension[MainDenyWithCode].get
      val denyCode = extension.getAcl.getDenyCode
      denyCode shouldBe Acl.DenyStatusCode.CONFLICT.value
    }

    "generate a default ACL file descriptor with allow all services" in {
      val extension = lookupExtension[MainAllowAllServices].get
      val service = extension.getAcl.getAllow(0).getService
      service shouldBe "*"
    }

    "generate a default ACL file descriptor with allow two services" in {
      val extension = lookupExtension[MainAllowListOfServices].get
      val service1 = extension.getAcl.getAllow(0).getService
      service1 shouldBe "foo"

      val service2 = extension.getAcl.getAllow(1).getService
      service2 shouldBe "bar"
    }

    "generate a default ACL file descriptor with allow Principal INTERNET" in {
      val extension = lookupExtension[MainAllowPrincipalInternet].get
      val principal = extension.getAcl.getAllow(0).getPrincipal
      principal shouldBe PrincipalMatcher.Principal.INTERNET
    }

    "generate a default ACL file descriptor with allow Principal ALL" in {
      val extension = lookupExtension[MainAllowPrincipalAll].get
      val principal = extension.getAcl.getAllow(0).getPrincipal
      principal shouldBe PrincipalMatcher.Principal.ALL
    }

    "fail if both Principal and Service are defined" in {
      intercept[IllegalArgumentException] {
        lookupExtension[MainWithInvalidAllowAnnotation]
      }.getMessage shouldBe AclDescriptorFactory.invalidAnnotationUsage
    }

    "generate a default ACL file descriptor with deny all services" in {
      val extension = lookupExtension[MainDenyAllServices].get
      val service = extension.getAcl.getDeny(0).getService
      service shouldBe "*"
    }

    "generate a default ACL file descriptor with deny two services" in {
      val extension = lookupExtension[MainDenyListOfServices].get
      val service1 = extension.getAcl.getDeny(0).getService
      service1 shouldBe "foo"

      val service2 = extension.getAcl.getDeny(1).getService
      service2 shouldBe "bar"
    }

    "generate a default ACL file descriptor with deny Principal INTERNET" in {
      val extension = lookupExtension[MainDenyPrincipalInternet].get
      val principal = extension.getAcl.getDeny(0).getPrincipal
      principal shouldBe PrincipalMatcher.Principal.INTERNET
    }

    "generate a default ACL file descriptor with deny Principal ALL" in {
      val extension = lookupExtension[MainDenyPrincipalAll].get
      val principal = extension.getAcl.getDeny(0).getPrincipal
      principal shouldBe PrincipalMatcher.Principal.ALL
    }

    "fail if both Principal and Service are defined in 'deny' field" in {
      intercept[IllegalArgumentException] {
        lookupExtension[MainWithInvalidDenyAnnotation]
      }.getMessage shouldBe AclDescriptorFactory.invalidAnnotationUsage
    }
  }

}
