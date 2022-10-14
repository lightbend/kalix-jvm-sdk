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

import scala.reflect.ClassTag

import kalix.PrincipalMatcher
import kalix.springsdk.testmodels.AclTestModels.MainAllowAllServices
import kalix.springsdk.testmodels.AclTestModels.MainAllowListOfServices
import kalix.springsdk.testmodels.AclTestModels.MainAllowPrincipalAll
import kalix.springsdk.testmodels.AclTestModels.MainAllowPrincipalInternet
import kalix.springsdk.testmodels.AclTestModels.MainDenyAllServices
import kalix.springsdk.testmodels.AclTestModels.MainDenyListOfServices
import kalix.springsdk.testmodels.AclTestModels.MainDenyPrincipalAll
import kalix.springsdk.testmodels.AclTestModels.MainDenyPrincipalInternet
import kalix.springsdk.testmodels.AclTestModels.MainDenyWithCode
import kalix.springsdk.testmodels.AclTestModels.MainWithInvalidAllowAnnotation
import kalix.springsdk.testmodels.AclTestModels.MainWithInvalidDenyAnnotation
import kalix.springsdk.testmodels.AclTestModels.MainWithoutAnnotation
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AclDescriptorFactorySpec extends AnyWordSpec with Matchers {

  def lookupExtention[T: ClassTag] =
    AclDescriptorFactory
      .defaultAclFileDescriptor(implicitly[ClassTag[T]].runtimeClass)
      .map { aclFileDesc =>

        aclFileDesc.getOptions.getExtension(kalix.Annotations.file)
      }

  "The DefaultAclDescriptorFactory" should {

    "generate an empty descriptor if no ACL annotation is found" in {
      val extension = lookupExtention[MainWithoutAnnotation]
      extension shouldBe empty
    }

    "generate a default ACL file descriptor with deny code" in {
      val extension = lookupExtention[MainDenyWithCode].get
      val denyCode = extension.getAcl.getDenyCode
      denyCode shouldBe 7
    }

    "generate a default ACL file descriptor with allow all services" in {
      val extension = lookupExtention[MainAllowAllServices].get
      val service = extension.getAcl.getAllow(0).getService
      service shouldBe "*"
    }

    "generate a default ACL file descriptor with allow two services" in {
      val extension = lookupExtention[MainAllowListOfServices].get
      val service1 = extension.getAcl.getAllow(0).getService
      service1 shouldBe "foo"

      val service2 = extension.getAcl.getAllow(1).getService
      service2 shouldBe "bar"
    }

    "generate a default ACL file descriptor with allow Principal INTERNET" in {
      val extension = lookupExtention[MainAllowPrincipalInternet].get
      val principal = extension.getAcl.getAllow(0).getPrincipal
      principal shouldBe PrincipalMatcher.Principal.INTERNET
    }

    "generate a default ACL file descriptor with allow Principal ALL" in {
      val extension = lookupExtention[MainAllowPrincipalAll].get
      val principal = extension.getAcl.getAllow(0).getPrincipal
      principal shouldBe PrincipalMatcher.Principal.ALL
    }

    "fail if both Principal and Service are defined" in {
      intercept[IllegalArgumentException] {
        lookupExtention[MainWithInvalidAllowAnnotation]
      }.getMessage shouldBe AclDescriptorFactory.invalidAnnotationUsage
    }

    "generate a default ACL file descriptor with deny all services" in {
      val extension = lookupExtention[MainDenyAllServices].get
      val service = extension.getAcl.getDeny(0).getService
      service shouldBe "*"
    }

    "generate a default ACL file descriptor with deny two services" in {
      val extension = lookupExtention[MainDenyListOfServices].get
      val service1 = extension.getAcl.getDeny(0).getService
      service1 shouldBe "foo"

      val service2 = extension.getAcl.getDeny(1).getService
      service2 shouldBe "bar"
    }

    "generate a default ACL file descriptor with deny Principal INTERNET" in {
      val extension = lookupExtention[MainDenyPrincipalInternet].get
      val principal = extension.getAcl.getDeny(0).getPrincipal
      principal shouldBe PrincipalMatcher.Principal.INTERNET
    }

    "generate a default ACL file descriptor with deny Principal ALL" in {
      val extension = lookupExtention[MainDenyPrincipalAll].get
      val principal = extension.getAcl.getDeny(0).getPrincipal
      principal shouldBe PrincipalMatcher.Principal.ALL
    }

    "fail if both Principal and Service are defined in 'deny' field" in {
      intercept[IllegalArgumentException] {
        lookupExtention[MainWithInvalidDenyAnnotation]
      }.getMessage shouldBe AclDescriptorFactory.invalidAnnotationUsage
    }
  }

}
