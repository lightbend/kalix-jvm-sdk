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

import scala.jdk.CollectionConverters.ListHasAsScala

import kalix.springsdk.action.RestAnnotatedAction
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class ProtoDescriptorGeneratorSpec extends AnyWordSpecLike with Matchers with OptionValues {

  "ProtoDescriptorGenerator" should {
    "generate a descriptor from an Action class" in {

      val descriptor = ProtoDescriptorGenerator.generateFileDescriptorAction(classOf[RestAnnotatedAction])
      // very simplistic test to start with
      val service = descriptor.getServices.get(0)
      service.getName shouldBe "RestAnnotatedAction"

      service.getMethods.size() shouldBe 4

      val sortedMethods = service.getMethods.asScala.sortBy(_.getFullName)

      sortedMethods.foreach { method =>

        method.getInputType.getFullName shouldBe "kalix.springsdk.action.Message"
        method.getOutputType.getFullName shouldBe "kalix.springsdk.action.Message"

        val httpOptions = method.getOptions.getExtension(com.google.api.AnnotationsProto.http)
        method.getName match {
          case "PostNumber"   => httpOptions.getPost shouldBe "/post/message"
          case "GetMessage"   => httpOptions.getGet shouldBe "/get/message"
          case "PutMessage"   => httpOptions.getPut shouldBe "/put/message"
          case "PatchMessage" => httpOptions.getPatch shouldBe "/patch/message"
        }
      }

    }
  }
}
